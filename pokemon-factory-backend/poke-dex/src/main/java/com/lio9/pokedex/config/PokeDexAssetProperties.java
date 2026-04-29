package com.lio9.pokedex.config;



import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pokemon-factory.assets")
public record PokeDexAssetProperties(String imageBaseUrl) {
}