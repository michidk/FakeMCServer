package me.michidk.FakeMCServer;

import java.io.*;
import java.net.Socket;

/**
 * @author michidk
 */

public class ResponderThread extends Thread
{

    private Socket socket;
    private boolean enabled;
    private DataInputStream in;
    private DataOutputStream out;

    private static String motd = "";

    public ResponderThread(Socket socket) throws IOException
    {
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
                @SuppressWarnings("unused")
                int length = ByteBufUtils.readVarInt(in);
                int id = ByteBufUtils.readVarInt(in);


                if (id == 0 && showMotd)
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
                    @SuppressWarnings("unused")
                    int version = ByteBufUtils.readVarInt(in);
                    String ip = ByteBufUtils.readUTF8(in);
                    int port = in.readUnsignedShort();
                    int nextState = ByteBufUtils.readVarInt(in);


                    if (nextState == 1)
                    {
                        showMotd = true;
                        Main.log.info("ping: " + ip + ":" + port);
                    }
                    else
                    {
                        if (Main.kickMessage == null || Main.kickMessage == "")
                        {
                            Main.log.warning("kickmessage is not initialized");
                            return;
                        }

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
                    long time = in.readLong();

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
        StringBuilder sb = new StringBuilder();

        //i know, thats a cheapy way.. but i have a better overview
        if (Main.verText == null || Main.version == "")
        {
            sb.append("{ \"version\": { \"name\": \"\", \"protocol\": 4 },");
        }
        else
        {
            sb.append("{ \"version\": { \"name\": \"" + Main.verText + "\", \"protocol\": 0 },");
        }

        if (Main.maxPlayers == null)
        {
            sb.append(" \"players\": { \"max\": " + 0 + ", \"online\": 0,");
        }
        else
        {
            sb.append(" \"players\": { \"max\": " + Main.maxPlayers + ", \"online\": 0,");
        }

        if (Main.players == null || Main.players == "")
        {
            sb.append(" \"sample\":[ {\"name\":\"\u0000\", \"id\":\"\"} ] },");
        }
        else
        {
            sb.append(" \"sample\":[ {\"name\":\"" + Main.players + "\", \"id\":\"\"} ] },");
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
