package com.opentouchgaming.androidcore.license;

import android.app.Activity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PackageVerif
{

    public static apkInfo packageSig(Activity act)
    {
        try
        {
            apkInfo ret = new PackageVerif.apkInfo();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            InputStream fis = new FileInputStream(act.getPackageResourcePath());
            int n = 0;
            byte[] buffer = new byte[8192];
            while (n != -1)
            {
                n = fis.read(buffer);
                ret.len += n;
                if (n > 0)
                {
                    digest.update(buffer, 0, n);
                }
            }
            fis.close();
            ret.sig = digest.digest();
            return ret;
        }
        catch (FileNotFoundException e)
        {
            return null;
        }
        catch (IOException e)
        {
            return null;
        }
        catch (NoSuchAlgorithmException e)
        {
            return null;
        }
    }

    public static String bytesToString(byte[] b)
    {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < b.length; i++)
        {
            hexString.append(String.format("%02x", (0xFF & b[i])).toUpperCase());
        }
        return hexString.toString();
    }

    public static class apkInfo
    {
        public int len;
        public byte[] sig;
    }
}
