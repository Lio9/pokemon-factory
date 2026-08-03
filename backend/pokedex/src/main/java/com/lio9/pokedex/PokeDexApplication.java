package com.lio9.pokedex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(excludeName = {
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration",
    "com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration",
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration",
    "com.baomidou.mybatisplus.autoconfigure.DdlAutoConfiguration"
})
@ComponentScan(basePackages = {"com.lio9.pokedex", "com.lio9.common"})
@ConfigurationPropertiesScan(basePackages = {"com.lio9.pokedex.config", "com.lio9.common.config"})
public class PokeDexApplication {
    public static void main(String[] args) {
        SpringApplication.run(PokeDexApplication.class, args);
    }
}
