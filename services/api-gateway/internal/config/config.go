package config

import (
	"log"
	"os"

	"github.com/joho/godotenv"
)

type Config struct {
	Port            string
	JWTAccessSecret string
	UserServiceURL  string
	OrderServiceURL string
	PaymentURL      string
}

func Load() *Config {
	_ = godotenv.Load()

	cfg := &Config{
		Port:            getEnv("PORT", "3000"),
		JWTAccessSecret: getEnv("JWT_ACCESS_SECRET", "secret"),
		UserServiceURL:  getEnv("USER_SERVICE_URL", "http://localhost:4001"),
		OrderServiceURL: getEnv("ORDER_SERVICE_URL", "http://localhost:4002"),
		PaymentURL:      getEnv("PAYMENT_URL", "http://localhost:4003"),
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
