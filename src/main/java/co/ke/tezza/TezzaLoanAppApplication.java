package co.ke.tezza;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
@EnableSwagger2
@EnableScheduling
@EnableJpaRepositories(repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
public class TezzaLoanAppApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        loadDotenvIntoSystemProperties();
        SpringApplication.run(TezzaLoanAppApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        loadDotenvIntoSystemProperties();
        return builder.sources(TezzaLoanAppApplication.class);
    }

    private static void loadDotenvIntoSystemProperties() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();

            dotenv.entries().forEach(entry ->
                    System.setProperty(entry.getKey(), entry.getValue())
            );

            System.out.println("✅ .env loaded into system properties (" + dotenv.entries().size() + " keys)");
        } catch (Exception e) {
            System.err.println("❌ Error loading .env file: " + e.getMessage());
        }
    }
}