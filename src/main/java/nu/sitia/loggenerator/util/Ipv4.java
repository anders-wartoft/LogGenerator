package nu.sitia.loggenerator.util;

import java.util.Random;

public class Ipv4 {
    /**
     * Convert an ipv4 address to long format
     * Credits to mkyong.com
     *
     * @param ipAddress The string to convert
     * @return The address as a java long
     */
    public static long ipv4ToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += ip * Math.pow(256, power);
        }
        return result;
    }

    /**
     * Convert a long to a String representation of an ipv4 address
     * Credits to mkyong.com
     *
     * @param ip The long to convert
     * @return A String of the same ip
     */
    public static String longToIpv4(long ip) {
        return ((ip >> 24) & 0xFF) + "."
                + ((ip >> 16) & 0xFF) + "."
                + ((ip >> 8) & 0xFF) + "."
                + (ip & 0xFF);
    }

    /**
     * Calculate how many addresses there are in a subnet
     *
     * @param subnet The CIDR value
     * @return Number of addresses in that subnet.
     */
    public static long nrValuesInSubnet(String subnet) {
        long cidr = Long.parseLong(subnet);
        long mask = 32 - cidr;
        return 1L << (mask + 1) - 1;
    }

    /**
     * Is an adress within a subnet?
     *
     * @param address1 Address 1
     * @param address2 Address 2
     * @param cidr     The subnet
     * @return true iff the addresses are in the same subnet.
     */
    public static boolean isInSubnet(String address1, String address2, String cidr) {
        long adr1 = ipv4ToLong(address1);
        long adr2 = ipv4ToLong(address2);
        long mask = 32 - Long.parseLong(cidr);
        long bitmask = 0xFFFFFFFF;
        // zero out the rightmost bits
        bitmask = bitmask >> mask << mask;
        return (adr1 & bitmask) == (adr2 & bitmask);
    }

    /**
     * Calculate the starting address of a subnet*
     *
     * @param ipv4 The ipv4 address
     * @param cidr The cidr value
     * @return The starting address for the subnet
     */
    public static long getStartingAddress(String ipv4, String cidr) {
        long address = ipv4ToLong(ipv4);
        long bitmask = 0xFFFFFFFF;
        long mask = 32 - Long.parseLong(cidr);
        // zero out the rightmost bits
        bitmask = bitmask >> mask << mask;
        return address & bitmask;
    }

    /**
     * Randomize an adress within a given subnet
     *
     * @param ipv4 An address in the subnet
     * @param cidr The subnet
     * @return A random address within the subnet
     */
    public static long getRandomIpv4(String ipv4, String cidr) {
        long startingAddress = getStartingAddress(ipv4, cidr);
        long nrValues = nrValuesInSubnet(cidr);
        if (nrValues > 1) {
            return new Random().nextLong(nrValues - 1) + startingAddress;
        }
        // Special case. /32 cidr. Return the ip number supplied as an argument
        return ipv4ToLong(ipv4);
    }
}
