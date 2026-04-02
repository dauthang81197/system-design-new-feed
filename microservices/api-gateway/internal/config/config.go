package config

import (
	"log"
	"os"

	"github.com/joho/godotenv"
)

type Config struct {
	Port            string
	JWTAccessSecret string

	AuthServiceURL     string
	PostsServiceURL    string
	FollowServiceURL   string
	NewsfeedServiceURL string
}

func Load() *Config {
	_ = godotenv.Load()

	cfg := &Config{
		Port:               getEnv("PORT", "4000"),
		JWTAccessSecret:    getEnv("JWT_ACCESS_SECRET", "secret"),
		AuthServiceURL:     getEnv("AUTH_SERVICE_URL", "http://localhost:4001"),
		PostsServiceURL:    getEnv("POST_SERVICE_URL", "http://localhost:4002"),
		FollowServiceURL:   getEnv("FOLLOW_SERVICE_URL", "http://localhost:4003"),
		NewsfeedServiceURL: getEnv("NEWS_FEED_SERVICE_URL", "http://localhost:4004"),
	}

	log.Printf("Config loaded: PORT=%s", cfg.Port)
	return cfg
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
