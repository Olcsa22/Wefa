package hu.lanoga.toolbox.geo1;

import hu.lanoga.toolbox.spring.ApplicationContextHelper;

public class Ip2LocationUtil {

    private Ip2LocationUtil() {
        //
    }

    public static ToolboxIpLocation findIp2LocationIpv4(final String ipAddress) {
        return ApplicationContextHelper.getBean(Ip2LocationRepository.class).findIp2LocationIpv4(ipToLong(ipAddress));
    }

//    public static ToolboxIpLocation findIp2LocationIpv6(final BigDecimal ipAddress) {
//        return ApplicationContextHelper.getBean(Ip2LocationRepository.class).findIp2LocationIpv6(ipAddress);
//    }

    private static long ipToLong(final String ipAddress) {

        String[] ipAddressInArray = ipAddress.split("\\.");

        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {

            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += ip * Math.pow(256, power);

        }

        return result;
    }

}
