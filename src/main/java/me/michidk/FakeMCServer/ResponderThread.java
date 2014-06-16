package me.michidk.FakeMCServer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

/**
 * @author michidk
 */

public class ResponderThread extends Thread
{

    private final Socket socket;
    private volatile boolean enabled = false;
    private final DataInputStream in;
    private final DataOutputStream out;

    private static volatile String motd = null;

    public ResponderThread(final Socket socket) throws IOException
    {
        if(socket == null) throw new NullPointerException();
        this.socket = socket;

        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        socket.setSoTimeout(3000); //3s

        motd = createMotd();
        this.enabled = true;
    }

    @Override
    public void run()
    {
        boolean showMotd = false;

        try
        {
            while (this.socket.isConnected() && this.enabled)
            {
                final int length   = ByteBufUtils.readVarInt(this.in);
                final int packetId = ByteBufUtils.readVarInt(this.in);

                Main.debug("length: "+length+"  packet id: "+packetId);

                if(packetId == 0)
                {
                    if (motd == null || motd == "")
                    {
                        Main.log.warning("motd is not initialized");
                        return;
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);

                    ByteBufUtils.writeVarInt(dos, 0);
                    ByteBufUtils.writeUTF8(dos, motd);
                    ByteBufUtils.writeVarInt(this.out, baos.size());

                    this.out.write(baos.toByteArray());
                    this.out.flush();
                }
                else if (packetId == 0 && !showMotd)
                {
                    final int version   = ByteBufUtils.readVarInt(this.in);
                    final String ip     = ByteBufUtils.readUTF8(this.in);
                    final int port      = this.in.readUnsignedShort();
                    final int nextState = ByteBufUtils.readVarInt(this.in);

                    Main.debug("protocol:"+protocol+" ip:"+ip+" port:"+port+" state:"+nextState);

                    System.out.println("State: " + nextState);
                    if (nextState == 1)
                    {
                        showMotd = true;
                        Main.log.info("ping: " + ip + ":" + port);
                    }
                    else
                    {
//                        if (Main.kickMessage == null || Main.kickMessage.isEmpty())
//                        {
//                            Main.log.warning("kickmessage is not initialized");
//                            return;
//                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);

                        ByteBufUtils.writeVarInt(dos, 0);
                        ByteBufUtils.writeUTF8(dos, "{text:\"" + Main.kickMessage + "\", color: white}");
                        ByteBufUtils.writeVarInt(this.out, baos.size());

                        this.out.write(baos.toByteArray());
                        this.out.flush();

                        closeSocket();

                        Main.log.info("kick: " + ip + ":" + port);
                    }
                }
                else if (packetId == 1)
                {
                    final long time = this.in.readLong();

                    ByteBufUtils.writeVarInt(this.out, 9);
                    ByteBufUtils.writeVarInt(this.out, 1);

                    this.out.writeLong(time);
                    this.out.flush();
                }

            }

        }
        catch (EOFException e)
        {
            //ignore this unnecessary error
            return;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


    }

    private final void closeSocket()
    {

        this.enabled = false;

        try
        {
            this.in.close();
            this.out.close();
            this.socket.close();
        }
        catch (Exception e)
        {
            Main.log.severe("failed to close socket: " + e.getMessage());
        }
        Thread.currentThread().interrupt();

    }

    private String createMotd()
    {
        final StringBuilder sb = new StringBuilder();

        //i know, thats a cheapy way.. but i have a better overview
        if (Main.verText == null || Main.version == "")
        {
            sb.append("{ \"version\": { \"name\": \"\", \"protocol\": 4 },");
        }
        else
        {
            sb.append("{ \"version\": { \"name\": \"" + Main.verText + "\", \"protocol\": 0 },");
        }

        // max players
        if (Main.maxPlayers == null)
        {

            sb.append(" \"players\": { \"max\": 0, \"online\": 0,");
            sb.append(" \"sample\":[ {\"name\":\"\u0000\", \"id\":\"\u0000\"} ] },");

        }
        else
        {
            sb.append(" \"players\": { \"max\": ").append(Main.maxPlayers).append(", \"online\": ");

            // players
            if (Main.players == null || Main.players.length == 0)
            {
                sb.append("0, \"sample\":[ {\"name\":\"\u0000\", \"id\":\"\u0000\"} ] },");
            }
            else
            {
                sb.append(Main.players.length).append(", \"sample\":[");
                int count = 0;
                for(final String player : Main.players)
                {
                    if(count != 0)
                        sb.append(", ");
                    count++;
// this can be replaced with a function to fetch and cache the real uuid's
final String uuid = UUID.randomUUID().toString();
                    sb.append(" {\"name\":\"").append(player).append("\", \"id\":\"" + uuid + "\"}");
                    if(count == 10 && Main.players.length > 10)
                        break;
                }
                sb.append(" ] },");
            }
        }

        if (Main.motd == null || Main.motd == "")
        {
            sb.append(" \"description\": {\"text\":\"\u0000\"}");
        }
        else
        {
            sb.append(" \"description\": {\"text\":\"" + Main.motd + "\"}");
        }


        if (Main.icon == null || Main.icon == "")
        {
            sb.append(", \"favicon\": \"" + Main.blankIcon + "\" }");
        }
        else
        {
            sb.append(", \"favicon\": \"" + Main.icon + "\" }");
        }

        return sb.toString();
    }


}
