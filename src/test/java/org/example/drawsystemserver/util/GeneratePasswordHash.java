package org.example.drawsystemserver.util;

/**
 * 用于生成BCrypt密码哈希的工具类
 * 运行main方法可以生成BCrypt加密后的密码字符串
 */
public class GeneratePasswordHash {
    public static void main(String[] args) {
        // 生成管理员密码（密码：123456）
        String adminPassword = "123456";
        String adminHash = PasswordUtil.encode(adminPassword);
        System.out.println("管理员密码哈希：");
        System.out.println(adminHash);
        System.out.println();

        // 生成队长密码（密码：123456）
        String captainPassword = "123456";
        String captainHash = PasswordUtil.encode(captainPassword);
        System.out.println("队长密码哈希：");
        System.out.println(captainHash);
        System.out.println();

        // 生成SQL插入语句
        System.out.println("-- 管理员");
        System.out.println("INSERT INTO `user` (`username`, `password`, `userType`) VALUES");
        System.out.println("('admin', '" + adminHash + "', 'ADMIN');");
        System.out.println();
        
        System.out.println("-- 队长");
        System.out.println("INSERT INTO `user` (`username`, `password`, `userType`) VALUES");
        for (int i = 1; i <= 4; i++) {
            String hash = PasswordUtil.encode(captainPassword);
            String comma = i < 4 ? "," : ";";
            System.out.println("('captain" + i + "', '" + hash + "', 'CAPTAIN')" + comma);
        }
    }
}
