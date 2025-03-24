package com.museum;

import com.museum.config.JsonResult;
import com.museum.domain.po.MsUser;
import com.museum.service.impl.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 生成测试用户和token的测试类
 */
@SpringBootTest
@Slf4j
public class TokenGeneratorTest {

    @Resource
    private UserService userService;

    // 生成随机数的实例
    private final Random random = new Random();
    // 原子整数用于生成唯一ID
    private final AtomicInteger counter = new AtomicInteger(1);

    @Test
    void generateUsersAndTokens() throws Exception {
        // 用户数量
        final int USER_COUNT = 200;
        // 用于保存所有token的列表
        List<String> tokens = new ArrayList<>(USER_COUNT);
        // 计数器，用于记录成功创建的用户数
        int successCount = 0;

        log.info("开始创建{}个测试用户...", USER_COUNT);

        for (int i = 0; i < USER_COUNT; i++) {
            try {
                // 创建用户
                MsUser user = createRandomUser();
                
                // 保存用户到数据库
                userService.saveMsUser(user, null);
                
                // 登录获取token
                String token = userService.login(user);
                if (token != null) {
                    tokens.add(token);
                    successCount++;
                    
                    // 每100个用户记录一次进度
                    if (successCount % 100 == 0) {
                        log.info("已成功创建 {} 个用户", successCount);
                    }
                }
            } catch (Exception e) {
                log.error("创建用户失败: {}", e.getMessage());
            }
        }

        log.info("用户创建完成，共成功创建 {} 个用户", successCount);
        
        // 将token保存到文件中
        saveTokensToFile(tokens);
    }

    /**
     * 创建一个随机的合法用户
     */
    private MsUser createRandomUser() {
        // 获取唯一计数器值
        int id = counter.getAndIncrement();
        
        MsUser user = new MsUser();
        // 设置用户名 (testuser + 唯一ID)
        user.setUsername("testuser" + id);
        // 设置昵称
        user.setNickname("测试用户" + id);
        // 设置密码 (统一设置为 123456)
        user.setPassword("123456");
        // 设置状态为正常 (0)
        user.setState(0);
        
        // 生成有效的手机号 (以1开头的11位数字)
        String mobile = "1" + (3 + random.nextInt(7)) + generateRandomDigits(9);
        user.setMobile(mobile);
        
        // 生成有效的身份证号 (18位)
        String idCard = generateRandomIdCard();
        user.setIdCard(idCard);
        
        return user;
    }

    /**
     * 生成指定长度的随机数字字符串
     */
    private String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成有效的身份证号
     * 身份证结构: 6位地区码 + 8位出生日期 + 3位序列号 + 1位校验码
     */
    private String generateRandomIdCard() {
        // 地区码 (使用一些常见的地区码)
        String[] areaCodes = {"110101", "310101", "440101", "500101", "330101"};
        String areaCode = areaCodes[random.nextInt(areaCodes.length)];
        
        // 出生日期 (1960-2000年之间)
        int year = 1960 + random.nextInt(40);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28); // 简化处理，避免月份天数问题
        
        String birthDate = String.format("%04d%02d%02d", year, month, day);
        
        // 3位序列号
        String sequence = generateRandomDigits(3);
        
        // 计算校验码 (简化处理，直接随机生成)
        String[] checkCodes = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "X"};
        String checkCode = checkCodes[random.nextInt(checkCodes.length)];
        
        return areaCode + birthDate + sequence + checkCode;
    }

    /**
     * 将token保存到文件中
     */
    private void saveTokensToFile(List<String> tokens) {
        // 修改保存路径到 src/test/resources 目录下
        String filePath = "src/test/resources/tokens.txt";
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String token : tokens) {
                writer.write(token);
                writer.newLine();
            }
            log.info("共有 {} 个token已保存到文件: {}", tokens.size(), filePath);
        } catch (IOException e) {
            log.error("保存token到文件失败: {}", e.getMessage());
        }
    }

    /**
     * 为数据库中已存在的用户获取token
     * 如果之前的创建过程中断，可以通过此方法继续获取token
     */
    @Test
    void generateTokensForExistingUsers() {
        // 从数据库中查询所有以"testuser"开头的用户
        List<MsUser> existingUsers = userService.lambdaQuery()
                .like(MsUser::getUsername, "testuser")
                .list();
                
        log.info("从数据库找到{}个测试用户", existingUsers.size());
        
        // 用于保存所有token的列表
        List<String> tokens = new ArrayList<>(existingUsers.size());
        // 计数器，用于记录成功获取token的数量
        int successCount = 0;
        
        for (MsUser user : existingUsers) {
            try {
                // 登录获取token
                String token = userService.login(user);
                if (token != null) {
                    tokens.add(token);
                    successCount++;
                    
                    // 每100个用户记录一次进度
                    if (successCount % 100 == 0) {
                        log.info("已成功获取 {} 个token", successCount);
                    }
                }
            } catch (Exception e) {
                log.error("用户[{}]获取token失败: {}", user.getUsername(), e.getMessage());
            }
        }
        
        log.info("token获取完成，共成功获取 {} 个token", successCount);
        
        // 将token保存到文件中
        saveTokensToFile(tokens);
    }

    /**
     * 分批为用户获取token
     * 每批处理的用户数量较少，减少系统负载
     */
    @Test
    void generateTokensInBatches() {
        // 从数据库中查询所有以"testuser"开头的用户
        List<MsUser> existingUsers = userService.lambdaQuery()
                .like(MsUser::getUsername, "testuser")
                .list();
                
        log.info("从数据库找到{}个测试用户", existingUsers.size());
        
        // 用于保存所有token的列表
        List<String> tokens = new ArrayList<>(existingUsers.size());
        // 每批处理的用户数量
        final int BATCH_SIZE = 50;
        
        for (int i = 0; i < existingUsers.size(); i += BATCH_SIZE) {
            // 计算当前批次的结束索引（不超过列表大小）
            int endIndex = Math.min(i + BATCH_SIZE, existingUsers.size());
            // 获取当前批次的用户
            List<MsUser> batchUsers = existingUsers.subList(i, endIndex);
            
            log.info("开始处理第{}批用户，共{}个", (i / BATCH_SIZE) + 1, batchUsers.size());
            
            for (MsUser user : batchUsers) {
                try {
                    // 登录获取token
                    String token = userService.login(user);
                    if (token != null) {
                        tokens.add(token);
                    }
                } catch (Exception e) {
                    log.error("用户[{}]获取token失败: {}", user.getUsername(), e.getMessage());
                }
            }
            
            // 每批处理完成后暂停一小段时间，减轻系统负载
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            log.info("第{}批处理完成，当前共获取{}个token", (i / BATCH_SIZE) + 1, tokens.size());
            
            // 每批次处理完成后就保存一次，避免全部处理完才保存导致中途中断丢失所有数据
            saveTokensToFile(tokens, "tokens_batch_" + ((i / BATCH_SIZE) + 1) + ".txt");
        }
        
        // 最后将所有token合并保存到主文件
        saveTokensToFile(tokens);
    }

    /**
     * 将token保存到指定文件中
     */
    private void saveTokensToFile(List<String> tokens, String fileName) {
        // 确保文件保存在 src/test/resources 目录下
        String filePath = "src/test/resources/" + fileName;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String token : tokens) {
                writer.write(token);
                writer.newLine();
            }
            log.info("共有 {} 个token已保存到文件: {}", tokens.size(), filePath);
        } catch (IOException e) {
            log.error("保存token到文件失败: {}", e.getMessage());
        }
    }
} 