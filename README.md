mc-account

EN

mc-account is an account management microservice developed as my part of a team social network project.
The project follows a microservice architecture. This service is responsible for user profile management: retrieving, updating, searching, blocking, unblocking and soft deleting accounts.
My responsibilities
* development of the mc-account microservice;
* implementation of REST API for user accounts;
* PostgreSQL integration with Spring Data JPA;
* database migrations with Liquibase;
* Eureka Discovery integration;
* communication with auth service via OpenFeign;
* Kafka event processing;
* unit and integration testing;
* Actuator and Prometheus metrics configuration;
* Docker configuration preparation.
Tech stack
* Java 21
* Spring Boot 3.2
* Spring Web
* Spring Data JPA
* Spring Security
* PostgreSQL
* Liquibase
* Kafka
* OpenFeign
* Eureka Client
* Docker
* Prometheus / Actuator
* JUnit 5
* Mockito
* Testcontainers
* JaCoCo
* SonarQube
Features
* get current user profile;
* update account data;
* search users;
* get account by ID;
* block and unblock users;
* soft delete account;
* get active user IDs;
* process user registration and user update events via Kafka.
 
RU

mc-account — микросервис управления аккаунтами пользователей, разработанный как моя часть командного проекта социальной сети.
Проект реализован в микросервисной архитектуре и отвечает за работу с пользовательскими профилями: получение, обновление, поиск, блокировку, разблокировку и мягкое удаление аккаунтов.
Моя зона ответственности
* разработка микросервиса mc-account;
* реализация REST API для аккаунтов пользователей;
* работа с PostgreSQL через Spring Data JPA;
* миграции базы данных через Liquibase;
* интеграция с Eureka Discovery;
* взаимодействие с auth-сервисом через OpenFeign;
* обработка Kafka-событий;
* покрытие логики тестами;
* подключение Actuator и Prometheus-метрик;
* подготовка Docker-конфигурации.
Стек
* Java 21
* Spring Boot 3.2
* Spring Web
* Spring Data JPA
* Spring Security
* PostgreSQL
* Liquibase
* Kafka
* OpenFeign
* Eureka Client
* Docker
* Prometheus / Actuator
* JUnit 5
* Mockito
* Testcontainers
* JaCoCo
* SonarQube
Основные возможности
* получение текущего профиля пользователя;
* обновление данных аккаунта;
* поиск пользователей;
* получение аккаунта по ID;
* блокировка и разблокировка пользователя;
* мягкое удаление аккаунта;
* получение списка активных пользователей;
* обработка событий регистрации и изменения пользователя через Kafka.
