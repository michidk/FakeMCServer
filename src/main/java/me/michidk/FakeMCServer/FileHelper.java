package me.michidk.FakeMCServer;

import javax.xml.bind.DatatypeConverter;
import java.io.*;

/**
 * @author michidk
 * from DKLib: https://github.com/michidk/DKLib
 */

public class FileHelper
{

    public static String decodeBase64(File file)
    {
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(file);
            byte[] imageBytes = new byte[(int)file.length()];
            fis.read(imageBytes);
            fis.close();
            return DatatypeConverter.printBase64Binary(imageBytes);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }


    public static void stringToFile(File file, String string)
    {
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(string);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (writer != null) {
                    writer.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static String stringFromFile(File file)
    {
        String line;
        StringBuilder stringBuilder = new StringBuilder();

        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(file));

            while ((line = reader.readLine()) != null)
            {
                stringBuilder.append(line);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return stringBuilder.toString();
    }



}
