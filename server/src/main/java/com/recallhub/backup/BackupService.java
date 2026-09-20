package com.recallhub.backup;

import com.recallhub.common.BusinessException;
import com.recallhub.config.RecallHubProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private final RecallHubProperties properties;
    private final Environment env;

    @Scheduled(cron = "0 15 2 * * *", zone = "${recallhub.timezone:Asia/Shanghai}")
    public void scheduled() {
        if (!properties.backup().enabled()) return;
        try { create(); } catch (Exception e) { log.error("Scheduled database backup failed", e); }
    }

    public BackupResult create() {
        try {
            LocalDate today = LocalDate.now();
            Path root = Paths.get(properties.backup().path()).toAbsolutePath().normalize();
            Path daily = root.resolve("daily"); Files.createDirectories(daily);
            Path output = daily.resolve("recallhub-" + today + ".sql");
            List<String> command = new ArrayList<>(List.of(properties.backup().mysqldump(),
                    "--host=" + value("MYSQL_HOST", "127.0.0.1"), "--port=" + value("MYSQL_PORT", "3306"),
                    "--user=" + value("MYSQL_USERNAME", "recallhub"), "--single-transaction", "--routines",
                    "--events", "--default-character-set=utf8mb4", "--result-file=" + output,
                    value("MYSQL_DATABASE", "recall_hub")));
            ProcessBuilder builder = new ProcessBuilder(command); builder.redirectErrorStream(true);
            builder.environment().put("MYSQL_PWD", value("MYSQL_PASSWORD", "recallhub"));
            Process process = builder.start();
            String processOutput = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (!process.waitFor(5, TimeUnit.MINUTES)) { process.destroyForcibly(); throw new BusinessException("BACKUP_TIMEOUT", "数据库备份超时"); }
            if (process.exitValue() != 0) throw new BusinessException("BACKUP_FAILED", "mysqldump 执行失败：" + processOutput);
            archive(root, output, today); prune(daily, 7); prune(root.resolve("weekly"), 4); prune(root.resolve("monthly"), 6);
            return new BackupResult(output.toString(), Files.size(output), OffsetDateTime.now());
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException("BACKUP_FAILED", "无法创建数据库备份：" + e.getMessage()); }
    }

    private void archive(Path root, Path daily, LocalDate date) throws Exception {
        if (date.getDayOfWeek() == DayOfWeek.MONDAY) {
            Path dir=root.resolve("weekly");Files.createDirectories(dir);
            String week=date.getYear()+"-W"+String.format("%02d",date.get(WeekFields.ISO.weekOfWeekBasedYear()));
            Files.copy(daily,dir.resolve("recallhub-"+week+".sql"),StandardCopyOption.REPLACE_EXISTING);
        }
        if (date.getDayOfMonth()==1) {
            Path dir=root.resolve("monthly");Files.createDirectories(dir);
            Files.copy(daily,dir.resolve("recallhub-"+date.format(DateTimeFormatter.ofPattern("yyyy-MM"))+".sql"),StandardCopyOption.REPLACE_EXISTING);
        }
    }
    private void prune(Path dir, int keep) throws Exception {
        if (!Files.exists(dir)) return;
        try (var stream=Files.list(dir)) {
            var files=stream.filter(Files::isRegularFile).sorted(Comparator.comparingLong(this::modified).reversed()).toList();
            for(int i=keep;i<files.size();i++) Files.deleteIfExists(files.get(i));
        }
    }
    private long modified(Path p){try{return Files.getLastModifiedTime(p).toMillis();}catch(Exception e){return 0;}}
    private String value(String key,String fallback){return Optional.ofNullable(env.getProperty(key)).filter(v->!v.isBlank()).orElse(fallback);}
    public record BackupResult(String file,long bytes,OffsetDateTime createdAt){}
}

