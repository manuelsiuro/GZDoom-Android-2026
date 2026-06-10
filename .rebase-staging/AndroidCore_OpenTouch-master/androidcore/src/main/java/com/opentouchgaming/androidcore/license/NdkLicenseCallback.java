package com.opentouchgaming.androidcore.license;

public interface NdkLicenseCallback
{

    int GOOD = 54;
    int NO_GOOD = 50;
    int ERROR = 90;

    void status(LicStatus ret);

    class LicStatus
    {
        public int code;
        public String desc;
    }
}
