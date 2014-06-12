package me.michidk.FakeMCServer;

import java.io.*;
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

        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        socket.setSoTimeout(3000); //3s

        motd = createMotd();
        enabled = true;
    }

    @Override
    public void run()
    {
        boolean showMotd = false;

        try
        {
            while (socket.isConnected() && enabled)
            {
                final int length = ByteBufUtils.readVarInt(in);
                final int id = ByteBufUtils.readVarInt(in);

                if (id == 0 && showMotd)
                Main.debug("length:"+length+" id:"+id);
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
                    ByteBufUtils.writeVarInt(out, baos.size());

                    out.write(baos.toByteArray());
                    out.flush();
                }
                else if (id == 0 && !showMotd)
                {
                    final int version = ByteBufUtils.readVarInt(in);
                    final String ip = ByteBufUtils.readUTF8(in);
                    final int port = in.readUnsignedShort();
                    final int nextState = ByteBufUtils.readVarInt(in);

                    Main.debug("protocol:"+protocol+" ip:"+ip+" port:"+port+" state:"+state);

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
                        ByteBufUtils.writeVarInt(out, baos.size());

                        out.write(baos.toByteArray());
                        out.flush();

                        closeSocket();

                        Main.log.info("kick: " + ip + ":" + port);
                    }
                }
                else if (id == 1)
                {
                    final long time = in.readLong();

                    ByteBufUtils.writeVarInt(out, 9);
                    ByteBufUtils.writeVarInt(out, 1);

                    out.writeLong(time);
                    out.flush();
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

        enabled = false;

        try
        {
            in.close();
            out.close();
            socket.close();
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
