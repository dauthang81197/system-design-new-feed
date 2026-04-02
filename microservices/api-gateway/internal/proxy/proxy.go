package proxy

import (
	"log"
	"net"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strings"
	"time"
)

func NewReverseProxy(upstream string, prefix string) http.Handler {
	target, err := url.Parse(upstream)
	if err != nil {
		log.Fatalf("Invalid upstream URL %s: %v", upstream, err)
	}

	proxy := httputil.NewSingleHostReverseProxy(target)

	originalDirector := proxy.Director

	proxy.Director = func(req *http.Request) {
		originalDirector(req)

		// Strip prefix (e.g. /auth/login → /login)
		if prefix != "" {
			req.URL.Path = strings.TrimPrefix(req.URL.Path, prefix)
			if req.URL.Path == "" {
				req.URL.Path = "/"
			}
		}

		// Add Forward headers
		req.Header.Set("X-Forwarded-Host", req.Host)
		req.Header.Set("X-Forwarded-Proto", "http")

		// Forward client IP
		if ip := clientIP(req); ip != "" {
			req.Header.Set("X-Forwarded-For", ip)
			req.Header.Set("X-Real-IP", ip)
		}

		// Add request ID if exists
		if reqID := req.Header.Get("X-Request-Id"); reqID != "" {
			req.Header.Set("X-Request-Id", reqID)
		}
	}

	// Optional: Transport tuning (faster)
	proxy.Transport = &http.Transport{
		MaxIdleConns:       100,
		IdleConnTimeout:    90 * time.Second,
		DisableCompression: false,
		ForceAttemptHTTP2:  true,
	}

	return proxy
}

// Extract client IP
func clientIP(r *http.Request) string {
	ip, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return ""
	}
	return ip
}
