package me.megamichiel.animationlib.bungee.category;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.megamichiel.animationlib.bungee.AnimLibPlugin;
import me.megamichiel.animationlib.bungee.RegisteredPlaceholder;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PingerCategory extends PlaceholderCategory {

    private final Map<String, Pinger> pingers = new ConcurrentHashMap<>();
    private final Map<String, Function<Pinger, RegisteredPlaceholder>> valid = new ConcurrentHashMap<>();

    public PingerCategory() {
        super("pinger");
    }

    @Override
    public void onEnable(AnimLibPlugin plugin) {
        valid.put("isonline", pinger -> (n, p) -> pinger.isOnline ? plugin.booleanTrue() : plugin.booleanFalse());
        valid.put("players",  pinger -> (n, p) -> Integer.toString(pinger.online));
        valid.put("max",      pinger -> (n, p) -> Integer.toString(pinger.max));
        valid.put("motd",     pinger -> (n, p) -> pinger.motd);
    }

    @Override
    public RegisteredPlaceholder get(String value) {
        int index = value.indexOf('_');
        if (index != -1) {
            String key = value.substring(0, index);
            Function<Pinger, RegisteredPlaceholder> func = valid.get(key);
            if (func != null) return func.apply(pinger(key));
        }
        return (n, p) -> "<unknown_pinger>";
    }

    private Pinger pinger(String address) {
        Pinger pinger = pingers.get(address);
        if (pinger == null) pingers.put(address, pinger = new Pinger(resolveAddress(address)));
        return pinger;
    }

    private InetSocketAddress resolveAddress(String str) {
        int index = str.indexOf(':');
        if (index != -1) {
            String ip = str.substring(0, index), port = str.substring(index + 1);
            try {
                return new InetSocketAddress(ip, Integer.parseInt(port));
            } catch (NumberFormatException ex) {
                System.err.println("Invalid pinger port in " + str + ": " + port);
                return new InetSocketAddress(ip, 25565);
            }
        } else return new InetSocketAddress(str, 25565);
    }

    private class Pinger {

        private final InetSocketAddress address;
        private boolean isOnline;
        private int online, max;
        private String motd;

        Pinger(InetSocketAddress address) {
            this.address = address;
        }

        void connect() {
            try (Socket socket = new Socket()) {
                socket.connect(address, 30_000);
                OutputStream output = socket.getOutputStream();
                InputStream input = socket.getInputStream();

                String address = this.address.getHostString();
                byte[] addressBytes = address.getBytes(Charsets.UTF_8);

                ByteArrayOutputStream handshake = new ByteArrayOutputStream();
                writeVarInt(handshake, 5
                        + varIntLength(addressBytes.length)
                        + addressBytes.length);
                handshake.write(new byte[] { 0, 47 }); // Packet ID + Protocol Version
                writeVarInt(handshake, addressBytes.length);
                handshake.write(addressBytes);
                int port = this.address.getPort();
                handshake.write((port >> 8) & 0xFF);
                handshake.write(port & 0xFF);
                handshake.write(1); // Next protocol state: Status
                output.write(handshake.toByteArray());

                // Request
                output.write(new byte[] { 1, 0 }); // Length + Packet ID

                // Response
                readVarInt(input);
                if (input.read() != 0) { // Weird Packet ID
                    socket.close();
                    isOnline = false;
                    return;
                }
                byte[] jsonBytes = new byte[readVarInt(input)];
                new DataInputStream(input).readFully(jsonBytes);
                JsonElement response = new JsonParser().parse(new String(jsonBytes, Charsets.UTF_8));
                String motd;
                int online = 0, max = 0;
                if (response.isJsonObject()) {
                    JsonObject obj = response.getAsJsonObject();
                    JsonElement desc = obj.get("description");
                    if (desc.isJsonObject())
                        motd = desc.getAsJsonObject().get("text").getAsString();
                    else motd = desc.getAsString();
                    JsonElement players = obj.get("players");
                    if (players.isJsonObject()) {
                        JsonObject playersObj = players.getAsJsonObject();
                        online = playersObj.get("online").getAsInt();
                        max = playersObj.get("max").getAsInt();
                    }
                } else motd = response.getAsString();

                this.isOnline = true;
                this.motd = motd;
                this.online = online;
                this.max = max;

                byte[] write = new byte[10];
                write[0] = 9;
                write[1] = 1;
                int index = 2;
                long time = System.currentTimeMillis();
                for (int i = 7; i >= 0; i--)
                    write[index++] = (byte) ((time >> (i * 8)) & 0xFF);
                output.write(write);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        void writeVarInt(OutputStream stream, int val) throws IOException {
            while ((val & 0xFFFFFF80) != 0) {
                stream.write(val & 0x7F | 0x80);
                val >>>= 7;
            }
            stream.write(val);
        }

        int readVarInt(InputStream stream) throws IOException {
            int result = 0, read;
            for (int i = 0; i < 5; i++) {
                read = stream.read();
                result |= (read & 0x7F) << (i * 7);
                if ((read & 0x80) == 0) return result;
            }
            throw new RuntimeException("VarInt too big");
        }

        int varIntLength(int i) {
            for (int j = 0; j < 5; j++)
                if ((i & (0xFFFFFFFF << (j * 7))) == 0)
                    return j;
            return 5;
        }
    }
}
