package main

import (
	"api-gateway/internal/config"
	"api-gateway/internal/proxy"
	"context"
	"log"
	"net/http"
	_ "os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"
)

func main() {
	cfg := config.Load()

	r := chi.NewRouter()

	// --- Health check ---
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte(`{"status":"ok"}`))
	})

	// --- 14 placeholder routes ---
	registerRoutes(r, cfg)

	server := &http.Server{
		Addr:    ":" + cfg.Port,
		Handler: r,
	}

	// Context bắt tín hiệu shutdown
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	go func() {
		log.Printf("API Gateway running on port %s", cfg.Port)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Server failed: %v", err)
		}
	}()

	// Đợi signal
	<-ctx.Done()
	log.Println("Shutting down gracefully...")

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Fatalf("Shutdown error: %v", err)
	}

	log.Println("Server exited.")
}

func registerRoutes(r *chi.Mux, cfg *config.Config) {

	// --- Reverse Proxy Phase 2 ---
	authProxy := proxy.NewReverseProxy(cfg.AuthServiceURL, "/auth")
	postsProxy := proxy.NewReverseProxy(cfg.PostsServiceURL, "/posts")
	followProxy := proxy.NewReverseProxy(cfg.FollowServiceURL, "/follow")
	newsfeedProxy := proxy.NewReverseProxy(cfg.NewsfeedServiceURL, "/newsfeed")

	r.Route("/auth", func(r chi.Router) {
		r.Handle("/*", authProxy)
	})

	r.Route("/posts", func(r chi.Router) {
		r.Handle("/*", postsProxy)
	})

	r.Route("/follow", func(r chi.Router) {
		r.Handle("/*", followProxy)
	})

	r.Route("/newsfeed", func(r chi.Router) {
		r.Handle("/*", newsfeedProxy)
	})

	// --- 14 placeholder routes ---
	r.Get("/users", placeholder)
	r.Get("/users/{id}", placeholder)
	r.Post("/users", placeholder)

	r.Post("/auth/login", placeholder)
	r.Post("/auth/login", placeholder)
	r.Post("/auth/register", placeholder)
	r.Post("/auth/refresh", placeholder)

	r.Get("/posts", placeholder)
	r.Post("/orders", placeholder)
	r.Get("/orders/{id}", placeholder)

	r.Get("/payments", placeholder)
	r.Post("/payments", placeholder)

	r.Get("/products", placeholder)
	r.Get("/products/{id}", placeholder)
	r.Post("/products", placeholder)
}

func placeholder(w http.ResponseWriter, r *http.Request) {
	w.Write([]byte("OK"))
}
