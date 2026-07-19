# About WaterFun

WaterFun is a feature-rich community forum platform built with a gateway-centric microservice architecture, dedicated to providing a secure, smooth, and diverse interactive experience. Whether you seek technical discussions, interest sharing, or casual conversations, WaterFun offers an open and inclusive space for everyone.

## Platform Highlights

### Secure Authentication

WaterFun employs a dual-token authentication mechanism combining RS256 asymmetric JWT with real-time Redis JTI validation, ensuring every request undergoes strict identity verification. Device fingerprint binding effectively prevents token theft and safeguards your account.

### Account Security

A comprehensive account security system including password setup and reset, email and phone number binding and modification, and captcha protection ensures every operation is secured.

### User Profiles

Each user can create a personalized profile page with custom avatar, nickname and bio. The follower system lets you keep track of interesting users, while privacy settings give you full control over your information visibility.

### Content Creation and Interaction

WaterFun enables you to freely publish and share content. The platform supports post creation and management, category and tag filtering, comments, likes, and bookmarks, creating a quality space for content creation and exchange.

### Notification System

The system inbox integrates unread counts, batch read, and SSE real-time push. Combined with RabbitMQ asynchronous delivery, every message reaches you promptly without missing any important notification.

### Ticket System

Content reports, account appeals, suggestions and feedback are submitted through a unified portal and processed via MQ asynchronous decoupling. Administrators review and execute actions automatically, notifying you of the results.

### Moderation

WaterFun features a comprehensive content moderation system supporting posts, images, text and other content types. The ban management system provides 6 penalty types with AOP @BanCheck annotation for automatic violation interception, along with auto-extension, permanent override, and flexible account restoration.

### Admin Dashboard

A comprehensive control panel for administrators covering user, role and permission management, site statistics, JVM real-time monitoring, online user tracking, and ECharts data visualization, making community operations effortless.

## Tech Stack

**Framework**: Spring Boot 4.0.x + Spring Cloud Gateway 2025.1.0

**Language**: Java 22 + TypeScript 5.9

**Frontend**: Nuxt 4 + Vue 3 (Client), Vue 3 + Vite 7 (Admin)

**UI**: Element Plus

**Auth**: Spring Security + OAuth2 Resource Server + RS256 JWT

**ORM**: JPA / Hibernate + MySQL

**Cache**: Caffeine (L1) → Redis (L2) → MySQL (L3) three-tier cache

**Queue**: RabbitMQ

**Storage**: Tencent COS (presigned URLs)

**SMS**: Aliyun SMS

**Email**: Resend + Spring Mail

## License

This project is open-sourced under the MPL-2.0 license. See LICENSE for details.

## Learn More

Website: [https://waterfun.top](https://waterfun.top)
