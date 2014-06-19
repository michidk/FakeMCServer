package me.michidk.FakeMCServer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.UUID;

/**
 * @author michidk
 */

public class ResponderThread extends Thread
{

    private volatile Thread thread = null;
    private final Socket socket;
    private final String remoteHost;
    private volatile boolean enabled = false;
    private final DataInputStream in;
    private final DataOutputStream out;

    public ResponderThread(final Socket socket) throws IOException
    {
        if(socket == null) throw new NullPointerException();
        this.socket = socket;
        this.remoteHost = socket.getRemoteSocketAddress().toString().substring(1);

        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        socket.setSoTimeout(3000); // 3s

        this.enabled = true;
    }

    @Override
    public void run()
    {
        this.thread = Thread.currentThread();
        boolean showMotd = false;
        int protocol = 5;

        try
        {
            int loopCount = 0;
            while (this.socket.isConnected() && this.enabled)
            {
                final int length   = ByteBufUtils.readVarInt(this.in);
                final int packetId = ByteBufUtils.readVarInt(this.in);

                loopCount++;
                if(loopCount == 1)
                    Main.debug();
                //Main.debug("length: "+length+"  packet id: "+packetId);

                if(length == 0)
                    return;

                // handshake
                if(packetId == 0)
                {
                    if(!showMotd) {
                        final int version = ByteBufUtils.readVarInt(this.in);
                        @SuppressWarnings("unused")
                        final String ip   = ByteBufUtils.readUTF8(this.in);
                        @SuppressWarnings("unused")
                        final int port    = this.in.readUnsignedShort();
                        final int state   = ByteBufUtils.readVarInt(this.in);
                        Main.debug("(state request) len:"+length+" id:"+packetId+" vers:"+version+" state:"+state);
                        protocol = version;
                        // state  1=status  2=login
                        if(state == 1)
                        {
                            // ping / status request
                            showMotd = true;
                            Main.log.info("ping: "+this.remoteHost);
                        }
                        else if(state == 2)
                        {
                            // login attempt
                            final String kickMsg = (Main.kickMessage == null || Main.kickMessage.isEmpty()) ?
                                    "Server is currently stopped, sorry." : Main.kickMessage;
                            writeData("{text:\""+kickMsg+"\", color: white}");
                            Main.log.info("kick: "+this.remoteHost+" - "+kickMsg);
                            return;
                        }
                    }
                    else
                    {
                        // motd packet
                        Main.debug("(motd requested) len:"+length+" id:"+packetId);
                        final String motd = createMotd(protocol);
                        if(motd == null || motd.isEmpty())
                        {
                            Main.log.warning("motd is not initialized");
                            return;
                        }
                        writeData(motd);
                        showMotd = false;
                    }
                    continue;
                }
                else if(packetId == 1)
                {
                    long lng = this.in.readLong();
                    Main.log.info("pong: "+lng);
                    ByteBufUtils.writeVarInt(this.out, 9);
                    ByteBufUtils.writeVarInt(this.out, 1);
                    this.out.writeLong(lng);
                    this.out.flush();
                    continue;
                }
                else
                {
                    Main.log.warning("Unknown packet: "+packetId);
                    return;
                }
            }
        }
        //ignore this unnecessary error
        catch (EOFException ignore)
        {
            Main.debug("(end of socket)");
        }
        catch (SocketTimeoutException ignore)
        {
            Main.debug("(socket timeout)");
        }
        catch (SocketException ignore)
        {
            Main.debug("(socket closed)");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            closeSocket();
            this.thread = null;
        }
    }

    private final void closeSocket()
    {
        this.enabled = false;
        Main.safeClose(this.in);
        Main.safeClose(this.out);
        Main.safeClose(this.socket);
        if(this.thread != null)
            this.thread.interrupt();
    }

    private void writeData(final String data) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream      dos  = new DataOutputStream(baos);
        ByteBufUtils.writeVarInt(dos, 0);
        ByteBufUtils.writeUTF8(dos, data);
        ByteBufUtils.writeVarInt(this.out, baos.size());
        this.out.write(baos.toByteArray());
        this.out.flush();
    }

    private String createMotd(final int protocolVersion)
    {
        final StringBuilder sb = new StringBuilder();

        // protocol version
        final String versionStr = (Main.verText == null || Main.verText.isEmpty()) ? "" : Main.verText;
        sb.append("{ \"version\": { \"name\": \"").append(versionStr).append("\", \"protocol\": ").append(protocolVersion).append(" },");

        // no max players
        if (Main.maxPlayers == null)
        {
            sb.append(" \"players\": { \"max\": 0, \"online\": 0,");
            sb.append(" \"sample\":[ {\"name\":\"\u0000\", \"id\":\"\u0000\"} ] },");
        }
        else
        {
            // max players
            sb.append(" \"players\": { \"max\": ").append(Main.maxPlayers).append(", \"online\": ");
            // no players online
            if (Main.players == null || Main.players.length == 0)
                sb.append("0, \"sample\":[ {\"name\":\"\u0000\", \"id\":\"\u0000\"} ] },");
            else
            {
                // players list
                sb.append(Main.players.length).append(", \"sample\":[");
                int count = 0;
                for(final String player : Main.players)
                {
                    if(count != 0)
                        sb.append(", ");
                    count++;
// this can be replaced with a function to fetch and cache the real uuid's
final String uuid = UUID.randomUUID().toString();
                    sb.append(" {\"name\":\"").append(player).append("\", \"id\":\"").append(uuid).append("\"}");
                    if(count == 10 && Main.players.length > 10)
                        break;
                }
                sb.append(" ] },");
            }
        }

        // motd text
        if (Main.motd == null || Main.motd.isEmpty())
            sb.append(" \"description\": {\"text\":\"\u0000\"}");
        else
            sb.append(" \"description\": {\"text\":\"").append(Main.motd).append("\"}");

        // server icon
        if (Main.icon == null || Main.icon.isEmpty())
            sb.append(", \"favicon\": \"").append(Main.blankIcon).append("\" }");
        else
            sb.append(", \"favicon\": \"").append(Main.icon).append("\" }");

        return sb.toString();
    }


}
