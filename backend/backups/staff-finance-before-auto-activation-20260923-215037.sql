-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: staff_finance
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `api_tokens`
--

DROP TABLE IF EXISTS `api_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `api_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_token_hash` (`token_hash`),
  KEY `FKcoalipytvnkd2pdqy0kseerct` (`user_id`),
  CONSTRAINT `FKcoalipytvnkd2pdqy0kseerct` FOREIGN KEY (`user_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `api_tokens`
--

LOCK TABLES `api_tokens` WRITE;
/*!40000 ALTER TABLE `api_tokens` DISABLE KEYS */;
INSERT INTO `api_tokens` VALUES (28,'2026-09-23 15:27:33.654353','2026-09-23 15:27:33.654353',0,'2026-09-23 23:27:33.606084','7988de82c30058b455e0d10e170277d314117e224e64c2e0ca22fcdbeddd4759',1),(50,'2026-09-23 19:47:18.719252','2026-09-23 19:47:18.719252',0,'2026-09-24 03:47:18.719252','64d7b1c43f8a0ef391b04f14e842135903e096b9621e9891834b2888652d85a5',1);
/*!40000 ALTER TABLE `api_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `app_users`
--

DROP TABLE IF EXISTS `app_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `email` varchar(160) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `full_name` varchar(160) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('ADMIN','APPROVER','INITIATOR','MEMBER') NOT NULL,
  `username` varchar(80) NOT NULL,
  `member_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_username` (`username`),
  UNIQUE KEY `UKqkweruflsfa061tq673soanm3` (`member_id`),
  CONSTRAINT `FK7t6womejfj554dbibpiv9uv4j` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `app_users`
--

LOCK TABLES `app_users` WRITE;
/*!40000 ALTER TABLE `app_users` DISABLE KEYS */;
INSERT INTO `app_users` VALUES (1,'2026-09-22 22:35:24.131088','2026-09-22 22:35:24.131088',0,'admin@example.org',_binary '','System Administrator','$2a$10$2z8nyi4jZvgyJJDz.xN/Pe1LxStNiwm0FKxCjw7O/UC4zXIO2J.ge','ADMIN','admin',NULL),(2,'2026-09-22 22:35:24.135087','2026-09-22 22:35:24.135087',0,'initiator@example.org',_binary '','Savings Initiator','$2a$10$A/eb3PPI6PPZLnXygRCy9.G3qbDpolfNPoqTbvOCqqcuIwX5V0uPC','INITIATOR','initiator',NULL),(3,'2026-09-22 22:35:24.136087','2026-09-22 22:35:24.136087',0,'approver@example.org',_binary '','Finance Approver','$2a$10$Xe0/4v6N2dFjafKdXiH3.uPoReZrIMDMvgWBc1T7RTMnu8XtV/yDW','APPROVER','approver',NULL),(4,'2026-09-22 22:35:24.138087','2026-09-22 22:35:24.138087',0,'alice@example.org',_binary '','Alice Mukamana','$2a$10$8PdTB73RxC5JypFZz1TWae2HbfgDhEc1iMYPS9BalYzxw/fuQuPye','MEMBER','member',1);
/*!40000 ALTER TABLE `app_users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `action` varchar(80) NOT NULL,
  `details` varchar(1000) DEFAULT NULL,
  `entity_id` bigint DEFAULT NULL,
  `entity_type` varchar(80) NOT NULL,
  `new_status` varchar(30) DEFAULT NULL,
  `previous_status` varchar(30) DEFAULT NULL,
  `reference` varchar(100) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_time` (`created_at`),
  KEY `FKqtxpcyjfyvcehqtn8n73di8du` (`user_id`),
  CONSTRAINT `FKqtxpcyjfyvcehqtn8n73di8du` FOREIGN KEY (`user_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=82 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
INSERT INTO `audit_logs` VALUES (1,'2026-09-22 23:06:35.806922','2026-09-22 23:06:35.806922',0,'UPDATE','Member information updated',1,'MEMBER','ACTIVE','ACTIVE','VFR01',1),(2,'2026-09-22 23:07:27.174439','2026-09-22 23:07:27.174439',0,'CREATE','INDIVIDUAL saving transaction created',7,'SAVING','DRAFT',NULL,'SAV-20260923-F57410',1),(3,'2026-09-22 23:07:27.222856','2026-09-22 23:07:27.222856',0,'SUBMIT','Submitted for approval',7,'SAVING','PENDING_APPROVAL','DRAFT','SAV-20260923-F57410',1),(4,'2026-09-22 23:21:56.436639','2026-09-22 23:21:56.436639',0,'APPROVE','OK',7,'SAVING','APPROVED','PENDING_APPROVAL','SAV-20260923-F57410',3),(5,'2026-09-22 23:23:06.980391','2026-09-22 23:23:06.980391',0,'CREATE','5 monthly contributions prepared',1,'SAVINGS_BATCH','DRAFT',NULL,'SB-1',2),(6,'2026-09-22 23:23:07.030488','2026-09-22 23:23:07.030488',0,'SUBMIT','Submitted for approval',1,'SAVINGS_BATCH','PENDING_APPROVAL','DRAFT','SB-1',2),(7,'2026-09-22 23:23:36.928466','2026-09-22 23:23:36.928466',0,'APPROVE','OKY',1,'SAVINGS_BATCH','APPROVED','PENDING_APPROVAL','SB-1',3),(8,'2026-09-22 23:24:51.606721','2026-09-22 23:24:51.606721',0,'CREATE','Loan application created for Alice Mukamana',2,'LOAN','DRAFT',NULL,'LOAN-20260923-A916F0',2),(9,'2026-09-22 23:24:51.661876','2026-09-22 23:24:51.661876',0,'SUBMIT','Submitted for approval',2,'LOAN','PENDING_APPROVAL','DRAFT','LOAN-20260923-A916F0',2),(10,'2026-09-22 23:25:23.809334','2026-09-22 23:25:23.809334',0,'APPROVE','OKEY',2,'LOAN','APPROVED','PENDING_APPROVAL','LOAN-20260923-A916F0',3),(11,'2026-09-22 23:27:30.032628','2026-09-22 23:27:30.032628',0,'DISBURSE','Loan disbursed using reference OK',2,'LOAN','ACTIVE','APPROVED','LOAN-20260923-A916F0',2),(12,'2026-09-22 23:28:55.216227','2026-09-22 23:28:55.216227',0,'CREATE','2 repayments prepared',1,'REPAYMENT_BATCH','DRAFT',NULL,'RB-1',2),(13,'2026-09-22 23:28:55.238172','2026-09-22 23:28:55.238172',0,'SUBMIT','Submitted for approval',1,'REPAYMENT_BATCH','PENDING_APPROVAL','DRAFT','RB-1',2),(14,'2026-09-22 23:29:48.303286','2026-09-22 23:29:48.303286',0,'APPROVE','',1,'REPAYMENT_BATCH','APPROVED','PENDING_APPROVAL','RB-1',1),(15,'2026-09-22 23:30:53.163902','2026-09-22 23:30:53.163902',0,'UPDATE','System setting updated',2,'SETTING','16','12.00','defaultLoanInterestRate',1),(16,'2026-09-22 23:37:46.837569','2026-09-22 23:37:46.837569',0,'CREATE','1 repayments prepared',2,'REPAYMENT_BATCH','DRAFT',NULL,'RB-2',1),(17,'2026-09-22 23:37:46.866238','2026-09-22 23:37:46.866238',0,'SUBMIT','Submitted for approval',2,'REPAYMENT_BATCH','PENDING_APPROVAL','DRAFT','RB-2',1),(18,'2026-09-22 23:39:07.586696','2026-09-22 23:39:07.586696',0,'APPROVE','OKEY',2,'REPAYMENT_BATCH','APPROVED','PENDING_APPROVAL','RB-2',3),(19,'2026-09-22 23:48:29.861282','2026-09-22 23:48:29.861282',0,'CREATE','2 repayments prepared',3,'REPAYMENT_BATCH','DRAFT',NULL,'RB-3',1),(20,'2026-09-22 23:48:29.880835','2026-09-22 23:48:29.880835',0,'SUBMIT','Submitted for approval',3,'REPAYMENT_BATCH','PENDING_APPROVAL','DRAFT','RB-3',1),(21,'2026-09-22 23:48:51.144969','2026-09-22 23:48:51.144969',0,'REJECT','OKY',3,'REPAYMENT_BATCH','REJECTED','PENDING_APPROVAL','RB-3',3),(22,'2026-09-22 23:58:40.278452','2026-09-22 23:58:40.278452',0,'CREATE','WITHDRAWAL saving transaction created',13,'SAVING','DRAFT',NULL,'okey',2),(23,'2026-09-22 23:58:40.298196','2026-09-22 23:58:40.298196',0,'SUBMIT','Submitted for approval',13,'SAVING','PENDING_APPROVAL','DRAFT','okey',2),(24,'2026-09-22 23:59:47.399539','2026-09-22 23:59:47.399539',0,'APPROVE','oket ',13,'SAVING','APPROVED','PENDING_APPROVAL','okey',3),(25,'2026-09-23 07:41:56.951985','2026-09-23 07:41:56.951985',0,'SUBMIT','Submitted for approval',3,'REPAYMENT_BATCH','PENDING_APPROVAL','REJECTED','RB-3',2),(26,'2026-09-23 08:30:21.772869','2026-09-23 08:30:21.772869',0,'CREATE','Loan application created for Diane Uwera',3,'LOAN','DRAFT',NULL,'LOAN-20260923-FAFCB6',1),(27,'2026-09-23 08:30:22.030747','2026-09-23 08:30:22.030747',0,'SUBMIT','Submitted for approval',3,'LOAN','PENDING_APPROVAL','DRAFT','LOAN-20260923-FAFCB6',1),(28,'2026-09-23 09:06:26.386372','2026-09-23 09:06:26.386372',0,'CREATE','Loan repayment prepared',7,'REPAYMENT','DRAFT',NULL,'PAY-20260923-E3E1A9',2),(29,'2026-09-23 09:06:26.477154','2026-09-23 09:06:26.477154',0,'SUBMIT','Submitted for approval',7,'REPAYMENT','PENDING_APPROVAL','DRAFT','PAY-20260923-E3E1A9',2),(30,'2026-09-23 09:06:31.969734','2026-09-23 09:06:31.969734',0,'CREATE','Loan repayment prepared',8,'REPAYMENT','DRAFT',NULL,'PAY-20260923-A685CC',2),(31,'2026-09-23 09:06:32.033336','2026-09-23 09:06:32.033336',0,'SUBMIT','Submitted for approval',8,'REPAYMENT','PENDING_APPROVAL','DRAFT','PAY-20260923-A685CC',2),(32,'2026-09-23 12:08:53.669991','2026-09-23 12:08:53.669991',0,'CREATE','5 monthly contributions prepared',2,'SAVINGS_BATCH','DRAFT',NULL,'SB-2',1),(33,'2026-09-23 12:08:53.703237','2026-09-23 12:08:53.703237',0,'SUBMIT','Submitted for approval',2,'SAVINGS_BATCH','PENDING_APPROVAL','DRAFT','SB-2',1),(34,'2026-09-23 15:20:27.918376','2026-09-23 15:20:27.918376',0,'CREATE','Loan application created for Eric Habimana',4,'LOAN','DRAFT',NULL,'LOAN-20260923-53FD7B',1),(35,'2026-09-23 15:20:28.032233','2026-09-23 15:20:28.032233',0,'SUBMIT','Submitted for approval',4,'LOAN','PENDING_APPROVAL','DRAFT','LOAN-20260923-53FD7B',1),(36,'2026-09-23 15:21:06.709444','2026-09-23 15:21:06.709444',0,'APPROVE','',4,'LOAN','APPROVED','PENDING_APPROVAL','LOAN-20260923-53FD7B',3),(37,'2026-09-23 15:23:28.771094','2026-09-23 15:23:28.771094',0,'DISBURSE','Loan disbursed using reference i',4,'LOAN','ACTIVE','APPROVED','LOAN-20260923-53FD7B',2),(38,'2026-09-23 15:24:25.427742','2026-09-23 15:24:25.427742',0,'CREATE','Loan repayment prepared',9,'REPAYMENT','DRAFT',NULL,'PAY-20260923-BB5C52',2),(39,'2026-09-23 15:24:25.552927','2026-09-23 15:24:25.552927',0,'SUBMIT','Submitted for approval',9,'REPAYMENT','PENDING_APPROVAL','DRAFT','PAY-20260923-BB5C52',2),(40,'2026-09-23 15:46:49.181109','2026-09-23 15:46:49.181109',0,'CREATE','Annual rate=17',2,'LOAN_CATEGORY','ACTIVE',NULL,'Bussiness loan',1),(41,'2026-09-23 16:23:50.326034','2026-09-23 16:23:50.326034',0,'CREATE','Loan repayment prepared',10,'REPAYMENT','DRAFT',NULL,'PAY-20260923-3B042D',1),(42,'2026-09-23 16:23:50.451301','2026-09-23 16:23:50.451301',0,'SUBMIT','Submitted for approval',10,'REPAYMENT','PENDING_APPROVAL','DRAFT','PAY-20260923-3B042D',1),(43,'2026-09-23 16:24:28.760398','2026-09-23 16:24:28.760398',0,'APPROVE','',10,'REPAYMENT','APPROVED','PENDING_APPROVAL','PAY-20260923-3B042D',3),(44,'2026-09-23 16:24:56.206398','2026-09-23 16:24:56.206398',0,'APPROVE','',2,'SAVINGS_BATCH','APPROVED','PENDING_APPROVAL','SB-2',3),(45,'2026-09-23 16:38:18.893488','2026-09-23 16:38:18.893488',0,'CREATE','5 monthly contributions prepared',1,'SAVINGS_BATCH','DRAFT',NULL,'SB-1',1),(46,'2026-09-23 16:38:18.943178','2026-09-23 16:38:18.943178',0,'SUBMIT','Submitted for approval',1,'SAVINGS_BATCH','PENDING_APPROVAL','DRAFT','SB-1',1),(47,'2026-09-23 16:38:49.252161','2026-09-23 16:38:49.252161',0,'UPDATE','Member information updated',6,'MEMBER','ACTIVE','LEFT','STF-006',1),(48,'2026-09-23 17:10:46.840097','2026-09-23 17:10:46.840097',0,'CREATE','6 monthly contributions prepared',2,'SAVINGS_BATCH','DRAFT',NULL,'SB-2',1),(49,'2026-09-23 17:10:46.892366','2026-09-23 17:10:46.892366',0,'SUBMIT','Submitted for approval',2,'SAVINGS_BATCH','PENDING_APPROVAL','DRAFT','SB-2',1),(50,'2026-09-23 17:11:43.035738','2026-09-23 17:11:43.035738',0,'REJECT','This Not True',1,'SAVINGS_BATCH','REJECTED','PENDING_APPROVAL','SB-1',3),(51,'2026-09-23 17:11:52.357876','2026-09-23 17:11:52.357876',0,'APPROVE','Okey',2,'SAVINGS_BATCH','APPROVED','PENDING_APPROVAL','SB-2',3),(52,'2026-09-23 17:13:43.192782','2026-09-23 17:13:43.192782',0,'CREATE','INDIVIDUAL saving transaction created',12,'SAVING','DRAFT',NULL,'Income of savbing',2),(53,'2026-09-23 17:13:43.245511','2026-09-23 17:13:43.245511',0,'SUBMIT','Submitted for approval',12,'SAVING','PENDING_APPROVAL','DRAFT','Income of savbing',2),(54,'2026-09-23 17:14:01.600814','2026-09-23 17:14:01.600814',0,'APPROVE','Okey',12,'SAVING','APPROVED','PENDING_APPROVAL','Income of savbing',3),(55,'2026-09-23 17:15:09.143341','2026-09-23 17:15:09.143341',0,'CREATE','WITHDRAWAL saving transaction created',13,'SAVING','DRAFT',NULL,'Penelities',2),(56,'2026-09-23 17:15:09.183820','2026-09-23 17:15:09.183820',0,'SUBMIT','Submitted for approval',13,'SAVING','PENDING_APPROVAL','DRAFT','Penelities',2),(57,'2026-09-23 17:15:50.429712','2026-09-23 17:15:50.429712',0,'APPROVE','Okey',13,'SAVING','APPROVED','PENDING_APPROVAL','Penelities',3),(58,'2026-09-23 17:17:20.071102','2026-09-23 17:17:20.071102',0,'UPDATE','Member information updated',6,'MEMBER','LEFT','ACTIVE','STF-006',1),(59,'2026-09-23 17:18:01.301505','2026-09-23 17:18:01.301505',0,'UPDATE','Member information updated',6,'MEMBER','ACTIVE','LEFT','STF-006',1),(60,'2026-09-23 17:19:18.747895','2026-09-23 17:19:18.747895',0,'CREATE','Loan application created for Alice Mukamana',1,'LOAN','DRAFT',NULL,'LOAN-20260923-1B49F8',1),(61,'2026-09-23 17:19:18.846214','2026-09-23 17:19:18.846214',0,'SUBMIT','Submitted for approval',1,'LOAN','PENDING_APPROVAL','DRAFT','LOAN-20260923-1B49F8',1),(62,'2026-09-23 17:20:02.824566','2026-09-23 17:20:02.824566',0,'APPROVE','This',1,'LOAN','APPROVED','PENDING_APPROVAL','LOAN-20260923-1B49F8',3),(63,'2026-09-23 17:36:55.611841','2026-09-23 17:36:55.611841',0,'DISBURSE','Loan disbursed using reference Test',1,'LOAN','ACTIVE','APPROVED','LOAN-20260923-1B49F8',2),(64,'2026-09-23 17:39:05.457358','2026-09-23 17:39:05.457358',0,'CREATE','Loan repayment prepared',1,'REPAYMENT','DRAFT',NULL,'PAY-20260923-CB8EE0',2),(65,'2026-09-23 17:39:05.546360','2026-09-23 17:39:05.546360',0,'SUBMIT','Submitted for approval',1,'FULL_SETTLEMENT','PENDING_APPROVAL','DRAFT','PAY-20260923-CB8EE0',2),(66,'2026-09-23 17:39:49.211587','2026-09-23 17:39:49.211587',0,'APPROVE','',1,'REPAYMENT','APPROVED','PENDING_APPROVAL','PAY-20260923-CB8EE0',3),(67,'2026-09-23 17:41:41.429179','2026-09-23 17:41:41.429179',0,'CREATE','Loan application created for Diane Uwera',2,'LOAN','DRAFT',NULL,'LOAN-20260923-B91F77',2),(68,'2026-09-23 17:41:41.561036','2026-09-23 17:41:41.561036',0,'SUBMIT','Submitted for approval',2,'LOAN','PENDING_APPROVAL','DRAFT','LOAN-20260923-B91F77',2),(69,'2026-09-23 17:42:20.711563','2026-09-23 17:42:20.711563',0,'APPROVE','',2,'LOAN','APPROVED','PENDING_APPROVAL','LOAN-20260923-B91F77',3),(70,'2026-09-23 17:42:55.445708','2026-09-23 17:42:55.445708',0,'DISBURSE','Loan disbursed using reference oook',2,'LOAN','ACTIVE','APPROVED','LOAN-20260923-B91F77',1),(71,'2026-09-23 17:43:28.176813','2026-09-23 17:43:28.176813',0,'CREATE','Loan repayment prepared',2,'REPAYMENT','DRAFT',NULL,'PAY-20260923-FCD1A7',1),(72,'2026-09-23 17:43:28.239001','2026-09-23 17:43:28.239001',0,'SUBMIT','Submitted for approval',2,'REPAYMENT','PENDING_APPROVAL','DRAFT','PAY-20260923-FCD1A7',1),(73,'2026-09-23 17:43:37.789071','2026-09-23 17:43:37.789071',0,'CREATE','Loan repayment prepared',3,'REPAYMENT','DRAFT',NULL,'PAY-20260923-A5F2D9',1),(74,'2026-09-23 17:43:37.845510','2026-09-23 17:43:37.845510',0,'SUBMIT','Submitted for approval',3,'REPAYMENT','PENDING_APPROVAL','DRAFT','PAY-20260923-A5F2D9',1),(75,'2026-09-23 17:43:49.686557','2026-09-23 17:43:49.686557',0,'CREATE','Loan repayment prepared',4,'REPAYMENT','DRAFT',NULL,'PAY-20260923-C092B2',1),(76,'2026-09-23 17:43:49.743630','2026-09-23 17:43:49.743630',0,'SUBMIT','Submitted for approval',4,'REPAYMENT','PENDING_APPROVAL','DRAFT','PAY-20260923-C092B2',1),(77,'2026-09-23 17:44:37.269819','2026-09-23 17:44:37.269819',0,'APPROVE','',2,'REPAYMENT','APPROVED','PENDING_APPROVAL','PAY-20260923-FCD1A7',3),(78,'2026-09-23 17:45:41.111854','2026-09-23 17:45:41.111854',0,'REJECT','jjj',4,'REPAYMENT','REJECTED','PENDING_APPROVAL','PAY-20260923-C092B2',3),(79,'2026-09-23 17:45:49.981460','2026-09-23 17:45:49.981460',0,'REJECT','jjj',3,'REPAYMENT','REJECTED','PENDING_APPROVAL','PAY-20260923-A5F2D9',3),(80,'2026-09-23 17:47:48.650611','2026-09-23 17:47:48.650611',0,'SUBMIT_CHANGE','Schedule version 2: this',2,'LOAN','TOP_UP',NULL,'LOAN-20260923-B91F77',2),(81,'2026-09-23 17:48:10.300554','2026-09-23 17:48:10.300554',0,'APPROVE_CHANGE','Additional principal=500000.00, term=5, effective=2026-09-23, remarks=',2,'LOAN','ACTIVE','TOP_UP','LOAN-20260923-B91F77',3);
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `finance_categories`
--

DROP TABLE IF EXISTS `finance_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `active` bit(1) NOT NULL,
  `category_type` enum('EXPENSE','INCOME') NOT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_categories`
--

LOCK TABLES `finance_categories` WRITE;
/*!40000 ALTER TABLE `finance_categories` DISABLE KEYS */;
INSERT INTO `finance_categories` VALUES (1,'2026-09-22 22:35:24.140089','2026-09-22 22:35:24.140089',0,_binary '','INCOME','Interest income'),(2,'2026-09-22 22:35:24.143088','2026-09-22 22:35:24.143088',0,_binary '','INCOME','Membership fees'),(3,'2026-09-22 22:35:24.145087','2026-09-22 22:35:24.145087',0,_binary '','EXPENSE','Bank charges'),(4,'2026-09-22 22:35:24.147086','2026-09-22 22:35:24.147086',0,_binary '','EXPENSE','Office supplies');
/*!40000 ALTER TABLE `finance_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `finance_transactions`
--

DROP TABLE IF EXISTS `finance_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `actioned_at` datetime(6) DEFAULT NULL,
  `decision_remarks` varchar(500) DEFAULT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `workflow_status` enum('APPROVED','DRAFT','PENDING_APPROVAL','REJECTED','REVERSED') NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `description` varchar(500) NOT NULL,
  `finance_type` enum('EXPENSE','INCOME') NOT NULL,
  `reference` varchar(80) NOT NULL,
  `supporting_reference` varchar(250) DEFAULT NULL,
  `transaction_date` date NOT NULL,
  `actioned_by_id` bigint DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  `submitted_by_id` bigint DEFAULT NULL,
  `category_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe93h8bm7qisux3xcm2q927exb` (`reference`),
  KEY `idx_finance_date` (`transaction_date`),
  KEY `FKgsrgndyn8xahvr4p4gsfc05lb` (`actioned_by_id`),
  KEY `FK1vc57t0fggdc53jrcoct5h008` (`created_by_id`),
  KEY `FK1s9if3gp4a4lmjr0e5otpqtek` (`submitted_by_id`),
  KEY `FKoxd2gxikq6ppcfoxgabcuglxm` (`category_id`),
  CONSTRAINT `FK1s9if3gp4a4lmjr0e5otpqtek` FOREIGN KEY (`submitted_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FK1vc57t0fggdc53jrcoct5h008` FOREIGN KEY (`created_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKgsrgndyn8xahvr4p4gsfc05lb` FOREIGN KEY (`actioned_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKoxd2gxikq6ppcfoxgabcuglxm` FOREIGN KEY (`category_id`) REFERENCES `finance_categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_transactions`
--

LOCK TABLES `finance_transactions` WRITE;
/*!40000 ALTER TABLE `finance_transactions` DISABLE KEYS */;
INSERT INTO `finance_transactions` VALUES (1,'2026-09-22 22:35:24.227088','2026-09-22 22:35:24.227088',0,'2026-09-22 22:35:24.226088',NULL,'2026-09-22 22:35:24.226088','APPROVED',450000.00,'Annual membership fees','INCOME','INC-20260923-2E0E3F',NULL,'2026-09-08',3,2,2,2),(2,'2026-09-22 22:35:24.231599','2026-09-22 22:35:24.231599',0,'2026-09-22 22:35:24.226088',NULL,'2026-09-22 22:35:24.226088','APPROVED',65000.00,'Quarterly bank fees','EXPENSE','EXP-20260923-658DDF',NULL,'2026-09-08',3,2,2,3);
/*!40000 ALTER TABLE `finance_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loan_categories`
--

DROP TABLE IF EXISTS `loan_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loan_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `annual_rate` decimal(7,3) NOT NULL,
  `name` varchar(100) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(255) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK89c04k051kbcugyayr43q2n6b` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loan_categories`
--

LOCK TABLES `loan_categories` WRITE;
/*!40000 ALTER TABLE `loan_categories` DISABLE KEYS */;
INSERT INTO `loan_categories` VALUES (1,'2026-09-23 08:29:29.663537','2026-09-23 08:29:29.663537',0,16.000,'Emmegeny Loan',1,NULL,NULL,NULL),(2,'2026-09-23 15:46:49.140669','2026-09-23 15:46:49.140669',0,17.000,'Bussiness loan',1,'System Administrator','business ','System Administrator');
/*!40000 ALTER TABLE `loan_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loan_repayments`
--

DROP TABLE IF EXISTS `loan_repayments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loan_repayments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `actioned_at` datetime(6) DEFAULT NULL,
  `decision_remarks` varchar(500) DEFAULT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `workflow_status` enum('APPROVED','DRAFT','PENDING_APPROVAL','REJECTED','REVERSED') NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `payment_date` date NOT NULL,
  `reference` varchar(80) NOT NULL,
  `remarks` varchar(500) DEFAULT NULL,
  `actioned_by_id` bigint DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  `submitted_by_id` bigint DEFAULT NULL,
  `batch_id` bigint DEFAULT NULL,
  `loan_id` bigint NOT NULL,
  `charges_paid` decimal(19,2) DEFAULT NULL,
  `expected_installment` decimal(19,2) DEFAULT NULL,
  `installment_number` int DEFAULT NULL,
  `interest_paid` decimal(19,2) DEFAULT NULL,
  `interest_waived` decimal(19,2) DEFAULT NULL,
  `outstanding_before` decimal(19,2) DEFAULT NULL,
  `payment_type` varchar(255) DEFAULT NULL,
  `principal_paid` decimal(19,2) DEFAULT NULL,
  `schedule_id` bigint DEFAULT NULL,
  `schedule_version_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKb971qvw80m5lc2kcwpm89rwa4` (`reference`),
  KEY `idx_repayment_loan` (`loan_id`),
  KEY `idx_repayment_date` (`payment_date`),
  KEY `FKqcxap94acs4cf51tfeny971w5` (`actioned_by_id`),
  KEY `FK5ssynb3n9af8lmyiwksnorrbc` (`created_by_id`),
  KEY `FKcs8g4ifb2n8v5ax041krtaqnk` (`submitted_by_id`),
  KEY `FKpqh6ypj3xqkdf51o2fs5b7udf` (`batch_id`),
  CONSTRAINT `FK5ssynb3n9af8lmyiwksnorrbc` FOREIGN KEY (`created_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKcs8g4ifb2n8v5ax041krtaqnk` FOREIGN KEY (`submitted_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKmvfjvk48bhsvwbdis0s9uwn1t` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`),
  CONSTRAINT `FKpqh6ypj3xqkdf51o2fs5b7udf` FOREIGN KEY (`batch_id`) REFERENCES `repayment_batches` (`id`),
  CONSTRAINT `FKqcxap94acs4cf51tfeny971w5` FOREIGN KEY (`actioned_by_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loan_repayments`
--

LOCK TABLES `loan_repayments` WRITE;
/*!40000 ALTER TABLE `loan_repayments` DISABLE KEYS */;
INSERT INTO `loan_repayments` VALUES (1,'2026-09-23 17:39:05.455337','2026-09-23 17:39:49.290834',2,'2026-09-23 17:39:49.211587','','2026-09-23 17:39:05.546360','APPROVED',101333.33,'2026-09-23','PAY-20260923-CB8EE0','This reson ',3,2,2,NULL,1,0.00,NULL,NULL,1333.33,7543.70,108877.03,'FULL_SETTLEMENT',100000.00,NULL,1),(2,'2026-09-23 17:43:28.166280','2026-09-23 17:44:37.281183',2,'2026-09-23 17:44:37.269819','','2026-09-23 17:43:28.234589','APPROVED',510022.08,'2026-09-23','PAY-20260923-FCD1A7','Okey',3,1,1,NULL,2,0.00,510022.08,1,13333.33,NULL,1020044.15,'INSTALLMENT',496688.75,13,2),(3,'2026-09-23 17:43:37.785128','2026-09-23 17:45:49.989581',2,'2026-09-23 17:45:49.981460','jjj','2026-09-23 17:43:37.845510','REJECTED',510022.08,'2026-09-23','PAY-20260923-A5F2D9','yhi',3,1,1,NULL,2,0.00,510022.08,1,13333.33,NULL,1020044.15,'INSTALLMENT',496688.75,13,2),(4,'2026-09-23 17:43:49.686557','2026-09-23 17:45:41.119054',2,'2026-09-23 17:45:41.111854','jjj','2026-09-23 17:43:49.743630','REJECTED',510022.08,'2026-09-23','PAY-20260923-C092B2','jjj',3,1,1,NULL,2,0.00,510022.08,1,13333.33,NULL,1020044.15,'INSTALLMENT',496688.75,13,2);
/*!40000 ALTER TABLE `loan_repayments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loan_schedule_versions`
--

DROP TABLE IF EXISTS `loan_schedule_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loan_schedule_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `actioned_at` datetime(6) DEFAULT NULL,
  `decision_remarks` varchar(500) DEFAULT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `workflow_status` enum('APPROVED','DRAFT','PENDING_APPROVAL','REJECTED','REVERSED') NOT NULL,
  `additional_amount` decimal(19,2) DEFAULT NULL,
  `annual_rate` decimal(7,3) DEFAULT NULL,
  `effective_date` date DEFAULT NULL,
  `first_installment_date` date DEFAULT NULL,
  `installment` decimal(19,2) DEFAULT NULL,
  `interest` decimal(19,2) DEFAULT NULL,
  `loan_id` bigint NOT NULL,
  `previous_principal` decimal(19,2) DEFAULT NULL,
  `previous_term` int NOT NULL,
  `principal` decimal(19,2) DEFAULT NULL,
  `remarks` varchar(1000) DEFAULT NULL,
  `schedule_type` varchar(255) DEFAULT NULL,
  `term` int NOT NULL,
  `total_payable` decimal(19,2) DEFAULT NULL,
  `version_number` int NOT NULL,
  `actioned_by_id` bigint DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  `submitted_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK799seie94uhx15x6w9ofc4504` (`loan_id`,`version_number`),
  KEY `FKrx0roco2238m30nf4d6gf29mp` (`actioned_by_id`),
  KEY `FKnhx4kp6t2dv6wiwsc46w86vep` (`created_by_id`),
  KEY `FKl64m09tj1wmv6s9b3av5wc2yp` (`submitted_by_id`),
  CONSTRAINT `FKl64m09tj1wmv6s9b3av5wc2yp` FOREIGN KEY (`submitted_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKnhx4kp6t2dv6wiwsc46w86vep` FOREIGN KEY (`created_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKrx0roco2238m30nf4d6gf29mp` FOREIGN KEY (`actioned_by_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loan_schedule_versions`
--

LOCK TABLES `loan_schedule_versions` WRITE;
/*!40000 ALTER TABLE `loan_schedule_versions` DISABLE KEYS */;
INSERT INTO `loan_schedule_versions` VALUES (1,'2026-09-23 17:19:18.628436','2026-09-23 17:20:02.851787',2,'2026-09-23 17:20:02.849313','This','2026-09-23 17:19:18.846214','APPROVED',0.00,16.000,'2026-09-23','2026-10-01',9073.09,8877.03,1,0.00,0,100000.00,'This ','ORIGINAL',12,108877.03,1,3,1,1),(2,'2026-09-23 17:41:41.403402','2026-09-23 17:42:20.740622',2,'2026-09-23 17:42:20.738407','','2026-09-23 17:41:41.561036','APPROVED',0.00,16.000,'2026-09-23','2026-10-01',510022.08,20044.15,2,0.00,0,1000000.00,'remail ','ORIGINAL',2,1020044.15,1,3,2,2),(3,'2026-09-23 17:47:48.601874','2026-09-23 17:48:10.313520',2,'2026-09-23 17:48:10.297250','','2026-09-23 17:47:48.642259','APPROVED',500000.00,16.000,'2026-09-23','2026-10-23',208759.61,40486.80,2,503311.25,1,1003311.25,'this','TOP_UP',5,1043798.05,2,3,2,2);
/*!40000 ALTER TABLE `loan_schedule_versions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loan_schedules`
--

DROP TABLE IF EXISTS `loan_schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loan_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `amount_paid` decimal(19,2) NOT NULL,
  `due_date` date NOT NULL,
  `expected_amount` decimal(19,2) NOT NULL,
  `installment_number` int NOT NULL,
  `payment_status` enum('OVERDUE','PAID','PARTIAL','PENDING','REPLACED','SETTLED') NOT NULL,
  `loan_id` bigint NOT NULL,
  `closing_balance` decimal(19,2) DEFAULT NULL,
  `interest_amount` decimal(19,2) DEFAULT NULL,
  `principal_amount` decimal(19,2) DEFAULT NULL,
  `interest_waived` decimal(19,2) DEFAULT NULL,
  `opening_balance` decimal(19,2) DEFAULT NULL,
  `schedule_version_id` bigint DEFAULT NULL,
  `sequence_number` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_loan_installment` (`loan_id`,`installment_number`),
  CONSTRAINT `FKrxbpyqjoyiki2bj16h8y471aa` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loan_schedules`
--

LOCK TABLES `loan_schedules` WRITE;
/*!40000 ALTER TABLE `loan_schedules` DISABLE KEYS */;
INSERT INTO `loan_schedules` VALUES (1,'2026-09-23 17:19:18.653279','2026-09-23 17:39:49.290834',1,9073.09,'2026-10-01',9073.09,1,'SETTLED',1,92260.24,1333.33,7739.76,0.00,100000.00,1,1),(2,'2026-09-23 17:19:18.662291','2026-09-23 17:39:49.290834',1,7842.95,'2026-11-01',9073.09,2,'SETTLED',1,84417.29,1230.14,7842.95,1230.14,92260.24,1,2),(3,'2026-09-23 17:19:18.668406','2026-09-23 17:39:49.290834',1,7947.53,'2026-12-01',9073.09,3,'SETTLED',1,76469.76,1125.56,7947.53,1125.56,84417.29,1,3),(4,'2026-09-23 17:19:18.678799','2026-09-23 17:39:49.290834',1,8053.49,'2027-01-01',9073.09,4,'SETTLED',1,68416.27,1019.60,8053.49,1019.60,76469.76,1,4),(5,'2026-09-23 17:19:18.684089','2026-09-23 17:39:49.298283',1,8160.87,'2027-02-01',9073.09,5,'SETTLED',1,60255.40,912.22,8160.87,912.22,68416.27,1,5),(6,'2026-09-23 17:19:18.693825','2026-09-23 17:39:49.298283',1,8269.68,'2027-03-01',9073.09,6,'SETTLED',1,51985.72,803.41,8269.68,803.41,60255.40,1,6),(7,'2026-09-23 17:19:18.695851','2026-09-23 17:39:49.298283',1,8379.95,'2027-04-01',9073.09,7,'SETTLED',1,43605.77,693.14,8379.95,693.14,51985.72,1,7),(8,'2026-09-23 17:19:18.711363','2026-09-23 17:39:49.298283',1,8491.68,'2027-05-01',9073.09,8,'SETTLED',1,35114.09,581.41,8491.68,581.41,43605.77,1,8),(9,'2026-09-23 17:19:18.711363','2026-09-23 17:39:49.298283',1,8604.90,'2027-06-01',9073.09,9,'SETTLED',1,26509.19,468.19,8604.90,468.19,35114.09,1,9),(10,'2026-09-23 17:19:18.725233','2026-09-23 17:39:49.298283',1,8719.63,'2027-07-01',9073.09,10,'SETTLED',1,17789.56,353.46,8719.63,353.46,26509.19,1,10),(11,'2026-09-23 17:19:18.727270','2026-09-23 17:39:49.300323',1,8835.90,'2027-08-01',9073.09,11,'SETTLED',1,8953.66,237.19,8835.90,237.19,17789.56,1,11),(12,'2026-09-23 17:19:18.736737','2026-09-23 17:39:49.300323',1,8953.66,'2027-09-01',9073.04,12,'SETTLED',1,0.00,119.38,8953.66,119.38,8953.66,1,12),(13,'2026-09-23 17:41:41.414665','2026-09-23 17:44:37.281183',1,510022.08,'2026-10-01',510022.08,1,'PAID',2,503311.25,13333.33,496688.75,0.00,1000000.00,2,1),(14,'2026-09-23 17:41:41.421049','2026-09-23 17:41:41.421049',0,0.00,'2026-11-01',510022.07,2,'PENDING',2,0.00,6710.82,503311.25,NULL,503311.25,2,2),(15,'2026-09-23 17:47:48.610552','2026-09-23 17:47:48.610552',0,0.00,'2026-10-23',208759.61,3,'PENDING',2,807929.12,13377.48,195382.13,NULL,1003311.25,3,1),(16,'2026-09-23 17:47:48.618184','2026-09-23 17:47:48.618184',0,0.00,'2026-11-23',208759.61,4,'PENDING',2,609941.90,10772.39,197987.22,NULL,807929.12,3,2),(17,'2026-09-23 17:47:48.622004','2026-09-23 17:47:48.622004',0,0.00,'2026-12-23',208759.61,5,'PENDING',2,409314.85,8132.56,200627.05,NULL,609941.90,3,3),(18,'2026-09-23 17:47:48.632403','2026-09-23 17:47:48.632403',0,0.00,'2027-01-23',208759.61,6,'PENDING',2,206012.77,5457.53,203302.08,NULL,409314.85,3,4),(19,'2026-09-23 17:47:48.634441','2026-09-23 17:47:48.634441',0,0.00,'2027-02-23',208759.61,7,'PENDING',2,0.00,2746.84,206012.77,NULL,206012.77,3,5);
/*!40000 ALTER TABLE `loan_schedules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loans`
--

DROP TABLE IF EXISTS `loans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `actioned_at` datetime(6) DEFAULT NULL,
  `decision_remarks` varchar(500) DEFAULT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `workflow_status` enum('APPROVED','DRAFT','PENDING_APPROVAL','REJECTED','REVERSED') NOT NULL,
  `annual_interest_rate` decimal(7,3) NOT NULL,
  `application_date` date NOT NULL,
  `application_number` varchar(50) NOT NULL,
  `approved_amount` decimal(19,2) DEFAULT NULL,
  `disbursement_date` date DEFAULT NULL,
  `disbursement_reference` varchar(80) DEFAULT NULL,
  `loan_status` enum('ACTIVE','APPROVED','CLOSED','COMPLETED','DRAFT','PENDING_APPROVAL','REJECTED') NOT NULL,
  `monthly_installment` decimal(19,2) DEFAULT NULL,
  `outstanding_balance` decimal(19,2) DEFAULT NULL,
  `purpose` varchar(500) NOT NULL,
  `remarks` varchar(1000) DEFAULT NULL,
  `repayment_months` int NOT NULL,
  `requested_amount` decimal(19,2) NOT NULL,
  `total_interest` decimal(19,2) DEFAULT NULL,
  `total_payable` decimal(19,2) DEFAULT NULL,
  `actioned_by_id` bigint DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  `submitted_by_id` bigint DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `adjustment_amount` decimal(38,2) DEFAULT NULL,
  `adjustment_date` date DEFAULT NULL,
  `adjustment_months` int DEFAULT NULL,
  `adjustment_remarks` varchar(255) DEFAULT NULL,
  `adjustment_requested_by` bigint DEFAULT NULL,
  `adjustment_requested_name` varchar(255) DEFAULT NULL,
  `adjustment_type` varchar(255) DEFAULT NULL,
  `category_name` varchar(255) DEFAULT NULL,
  `committed_member_id` bigint DEFAULT NULL,
  `interest_method` varchar(255) DEFAULT NULL,
  `active_principal` decimal(19,2) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `current_schedule_version_id` bigint DEFAULT NULL,
  `first_installment_date` date DEFAULT NULL,
  `installments_paid_before_schedule` int DEFAULT NULL,
  `interest_paid_before_schedule` decimal(19,2) DEFAULT NULL,
  `original_principal` decimal(19,2) DEFAULT NULL,
  `original_term` int DEFAULT NULL,
  `pending_schedule_version_id` bigint DEFAULT NULL,
  `principal_paid_before_schedule` decimal(19,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_loan_number` (`application_number`),
  UNIQUE KEY `UK4u7nx2ac37rrnviham02viryf` (`committed_member_id`),
  KEY `idx_loan_member` (`member_id`),
  KEY `FKshvacoefd8ujxy2j25pk6wfad` (`actioned_by_id`),
  KEY `FKsrkvbqkia8s69mnggais02dis` (`created_by_id`),
  KEY `FKbrycm11glb1il75tqf14wtrda` (`submitted_by_id`),
  CONSTRAINT `FKbrycm11glb1il75tqf14wtrda` FOREIGN KEY (`submitted_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKcx90n1minpb22v3jw4ojinqm` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`),
  CONSTRAINT `FKshvacoefd8ujxy2j25pk6wfad` FOREIGN KEY (`actioned_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKsrkvbqkia8s69mnggais02dis` FOREIGN KEY (`created_by_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loans`
--

LOCK TABLES `loans` WRITE;
/*!40000 ALTER TABLE `loans` DISABLE KEYS */;
INSERT INTO `loans` VALUES (1,'2026-09-23 17:19:18.568221','2026-09-23 17:39:49.290834',6,'2026-09-23 17:20:02.821658','This','2026-09-23 17:19:18.846214','APPROVED',16.000,'2026-09-23','LOAN-20260923-1B49F8',100000.00,'2026-09-23','Test','CLOSED',9073.09,0.00,'This','This ',12,100000.00,1333.33,101333.33,3,1,1,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Emmegeny Loan',NULL,'REDUCING_BALANCE',100000.00,1,1,'2026-10-01',NULL,NULL,100000.00,12,NULL,NULL),(2,'2026-09-23 17:41:41.375481','2026-09-23 17:48:10.313520',8,'2026-09-23 17:42:20.711563','','2026-09-23 17:41:41.561036','APPROVED',16.000,'2026-09-23','LOAN-20260923-B91F77',1500000.00,'2026-09-23','oook','ACTIVE',208759.61,1043798.05,'This test','remail ',5,1000000.00,53820.13,1553820.13,3,2,2,5,NULL,NULL,NULL,'this',NULL,NULL,NULL,'Emmegeny Loan',5,'REDUCING_BALANCE',1003311.25,1,3,'2026-10-23',1,13333.33,1000000.00,2,NULL,496688.75);
/*!40000 ALTER TABLE `loans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `department` varchar(120) NOT NULL,
  `email` varchar(160) NOT NULL,
  `exit_date` date DEFAULT NULL,
  `full_name` varchar(160) NOT NULL,
  `joining_date` date NOT NULL,
  `member_code` varchar(40) NOT NULL,
  `membership_status` enum('ACTIVE','DISABLED','LEFT') NOT NULL,
  `monthly_saving_amount` decimal(19,2) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `remarks` varchar(1000) DEFAULT NULL,
  `risk_status` enum('NORMAL','WATCHLIST') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_member_code` (`member_code`),
  KEY `idx_member_name` (`full_name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `members`
--

LOCK TABLES `members` WRITE;
/*!40000 ALTER TABLE `members` DISABLE KEYS */;
INSERT INTO `members` VALUES (1,'2026-09-22 22:35:23.718859','2026-09-22 23:06:35.730398',1,'Finance','alice@example.org',NULL,'Alice Mukamana','2024-09-23','VFR01','ACTIVE',50000.00,'0788000001','','NORMAL'),(2,'2026-09-22 22:35:23.755948','2026-09-22 22:35:23.755948',0,'Operations','patrick@example.org',NULL,'Patrick Niyonzima','2024-09-23','STF-002','ACTIVE',40000.00,'0788000002',NULL,'NORMAL'),(3,'2026-09-22 22:35:23.758987','2026-09-22 22:35:23.758987',0,'Customer Service','grace@example.org',NULL,'Grace Uwase','2024-09-23','STF-003','ACTIVE',50000.00,'0788000003',NULL,'NORMAL'),(4,'2026-09-22 22:35:23.760855','2026-09-22 22:35:23.760855',0,'ICT','eric@example.org',NULL,'Eric Habimana','2024-09-23','STF-004','ACTIVE',60000.00,'0788000004',NULL,'NORMAL'),(5,'2026-09-22 22:35:23.763851','2026-09-22 22:35:23.763851',0,'Administration','diane@example.org',NULL,'Diane Uwera','2024-09-23','STF-005','ACTIVE',35000.00,'0788000005',NULL,'NORMAL'),(6,'2026-09-22 22:35:23.764865','2026-09-23 17:18:01.301505',3,'Logisticss','jean@example.org',NULL,'Jean Pauel\r\nMugisha','2024-09-23','STF-006','ACTIVE',30000.00,'0788000006','','WATCHLIST');
/*!40000 ALTER TABLE `members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `repayment_allocations`
--

DROP TABLE IF EXISTS `repayment_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `repayment_allocations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `installment_number` int NOT NULL,
  `interest` decimal(19,2) DEFAULT NULL,
  `interest_waived` decimal(19,2) DEFAULT NULL,
  `principal` decimal(19,2) DEFAULT NULL,
  `repayment_id` bigint DEFAULT NULL,
  `schedule_id` bigint DEFAULT NULL,
  `schedule_version_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `repayment_allocations`
--

LOCK TABLES `repayment_allocations` WRITE;
/*!40000 ALTER TABLE `repayment_allocations` DISABLE KEYS */;
INSERT INTO `repayment_allocations` VALUES (1,'2026-09-23 17:39:49.217638','2026-09-23 17:39:49.217638',0,1,1333.33,0.00,7739.76,1,1,1),(2,'2026-09-23 17:39:49.232971','2026-09-23 17:39:49.232971',0,2,0.00,1230.14,7842.95,1,2,1),(3,'2026-09-23 17:39:49.242423','2026-09-23 17:39:49.242423',0,3,0.00,1125.56,7947.53,1,3,1),(4,'2026-09-23 17:39:49.242423','2026-09-23 17:39:49.242423',0,4,0.00,1019.60,8053.49,1,4,1),(5,'2026-09-23 17:39:49.251375','2026-09-23 17:39:49.251375',0,5,0.00,912.22,8160.87,1,5,1),(6,'2026-09-23 17:39:49.258082','2026-09-23 17:39:49.258082',0,6,0.00,803.41,8269.68,1,6,1),(7,'2026-09-23 17:39:49.263610','2026-09-23 17:39:49.263610',0,7,0.00,693.14,8379.95,1,7,1),(8,'2026-09-23 17:39:49.269664','2026-09-23 17:39:49.269664',0,8,0.00,581.41,8491.68,1,8,1),(9,'2026-09-23 17:39:49.273833','2026-09-23 17:39:49.273833',0,9,0.00,468.19,8604.90,1,9,1),(10,'2026-09-23 17:39:49.273833','2026-09-23 17:39:49.273833',0,10,0.00,353.46,8719.63,1,10,1),(11,'2026-09-23 17:39:49.283727','2026-09-23 17:39:49.283727',0,11,0.00,237.19,8835.90,1,11,1),(12,'2026-09-23 17:39:49.290834','2026-09-23 17:39:49.290834',0,12,0.00,119.38,8953.66,1,12,1),(13,'2026-09-23 17:44:37.281183','2026-09-23 17:44:37.281183',0,1,13333.33,0.00,496688.75,2,13,2);
/*!40000 ALTER TABLE `repayment_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `repayment_batches`
--

DROP TABLE IF EXISTS `repayment_batches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `repayment_batches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `actioned_at` datetime(6) DEFAULT NULL,
  `decision_remarks` varchar(500) DEFAULT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `workflow_status` enum('APPROVED','DRAFT','PENDING_APPROVAL','REJECTED','REVERSED') NOT NULL,
  `period` varchar(7) NOT NULL,
  `remarks` varchar(500) DEFAULT NULL,
  `total_amount` decimal(19,2) NOT NULL,
  `actioned_by_id` bigint DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  `submitted_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7ld663t7psmo9xititv2h8coa` (`actioned_by_id`),
  KEY `FKkryw21uq068c4vwelubdbciu5` (`created_by_id`),
  KEY `FKq7ft8p860agb64p45x7jy3qql` (`submitted_by_id`),
  CONSTRAINT `FK7ld663t7psmo9xititv2h8coa` FOREIGN KEY (`actioned_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKkryw21uq068c4vwelubdbciu5` FOREIGN KEY (`created_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKq7ft8p860agb64p45x7jy3qql` FOREIGN KEY (`submitted_by_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `repayment_batches`
--

LOCK TABLES `repayment_batches` WRITE;
/*!40000 ALTER TABLE `repayment_batches` DISABLE KEYS */;
/*!40000 ALTER TABLE `repayment_batches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `savings_batches`
--

DROP TABLE IF EXISTS `savings_batches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `savings_batches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `actioned_at` datetime(6) DEFAULT NULL,
  `decision_remarks` varchar(500) DEFAULT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `workflow_status` enum('APPROVED','DRAFT','PENDING_APPROVAL','REJECTED','REVERSED') NOT NULL,
  `period` varchar(7) NOT NULL,
  `remarks` varchar(500) DEFAULT NULL,
  `total_amount` decimal(19,2) NOT NULL,
  `actioned_by_id` bigint DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  `submitted_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbq25ax7wpnr7nn5kx994dcjow` (`actioned_by_id`),
  KEY `FKb2ecnnq1wcuohoqckmar7pwuq` (`created_by_id`),
  KEY `FKm2hnt8475phap8t7rrw8s0idq` (`submitted_by_id`),
  CONSTRAINT `FKb2ecnnq1wcuohoqckmar7pwuq` FOREIGN KEY (`created_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKbq25ax7wpnr7nn5kx994dcjow` FOREIGN KEY (`actioned_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKm2hnt8475phap8t7rrw8s0idq` FOREIGN KEY (`submitted_by_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `savings_batches`
--

LOCK TABLES `savings_batches` WRITE;
/*!40000 ALTER TABLE `savings_batches` DISABLE KEYS */;
INSERT INTO `savings_batches` VALUES (1,'2026-09-23 16:38:18.841528','2026-09-23 17:11:43.046157',3,'2026-09-23 17:11:43.035738','This Not True','2026-09-23 16:38:18.943178','REJECTED','2026-09','Monthly savings for 2026-09',235000.00,3,1,1),(2,'2026-09-23 17:10:46.778909','2026-09-23 17:11:52.362163',3,'2026-09-23 17:11:52.353825','Okey','2026-09-23 17:10:46.890348','APPROVED','2026-09','Monthly savings for 2026-09',240000.00,3,1,1);
/*!40000 ALTER TABLE `savings_batches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `savings_transactions`
--

DROP TABLE IF EXISTS `savings_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `savings_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `actioned_at` datetime(6) DEFAULT NULL,
  `decision_remarks` varchar(500) DEFAULT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `workflow_status` enum('APPROVED','DRAFT','PENDING_APPROVAL','REJECTED','REVERSED') NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `reference` varchar(80) NOT NULL,
  `saving_type` enum('ADJUSTMENT','INDIVIDUAL','MONTHLY','WITHDRAWAL') NOT NULL,
  `transaction_date` date NOT NULL,
  `actioned_by_id` bigint DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  `submitted_by_id` bigint DEFAULT NULL,
  `batch_id` bigint DEFAULT NULL,
  `member_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKlwuo59svv1haguxl6igrycpnh` (`reference`),
  KEY `idx_saving_member` (`member_id`),
  KEY `idx_saving_date` (`transaction_date`),
  KEY `FKlnnlnkyetvb18wbyfyb8fx9yb` (`actioned_by_id`),
  KEY `FKpt6ac6k0xdx7s8v1s0soi2dsg` (`created_by_id`),
  KEY `FKta9e4o8orho3knh6iyijayv60` (`submitted_by_id`),
  KEY `FKrbakia9bj179veied1yoiwbct` (`batch_id`),
  CONSTRAINT `FKcwbiysjebsr5jt8aupgj9majl` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`),
  CONSTRAINT `FKlnnlnkyetvb18wbyfyb8fx9yb` FOREIGN KEY (`actioned_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKpt6ac6k0xdx7s8v1s0soi2dsg` FOREIGN KEY (`created_by_id`) REFERENCES `app_users` (`id`),
  CONSTRAINT `FKrbakia9bj179veied1yoiwbct` FOREIGN KEY (`batch_id`) REFERENCES `savings_batches` (`id`),
  CONSTRAINT `FKta9e4o8orho3knh6iyijayv60` FOREIGN KEY (`submitted_by_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `savings_transactions`
--

LOCK TABLES `savings_transactions` WRITE;
/*!40000 ALTER TABLE `savings_transactions` DISABLE KEYS */;
INSERT INTO `savings_transactions` VALUES (1,'2026-09-23 16:38:18.843831','2026-09-23 17:11:43.046157',2,'2026-09-23 17:11:43.035738','This Not True','2026-09-23 16:38:18.943178','REJECTED',50000.00,'Monthly savings for 2026-09','SB-1-VFR01','MONTHLY','2026-09-30',3,1,1,1,1),(2,'2026-09-23 16:38:18.859913','2026-09-23 17:11:43.058524',2,'2026-09-23 17:11:43.035738','This Not True','2026-09-23 16:38:18.943178','REJECTED',35000.00,'Monthly savings for 2026-09','SB-1-STF-005','MONTHLY','2026-09-30',3,1,1,1,5),(3,'2026-09-23 16:38:18.873504','2026-09-23 17:11:43.059943',2,'2026-09-23 17:11:43.035738','This Not True','2026-09-23 16:38:18.943178','REJECTED',60000.00,'Monthly savings for 2026-09','SB-1-STF-004','MONTHLY','2026-09-30',3,1,1,1,4),(4,'2026-09-23 16:38:18.882252','2026-09-23 17:11:43.062290',2,'2026-09-23 17:11:43.035738','This Not True','2026-09-23 16:38:18.943178','REJECTED',50000.00,'Monthly savings for 2026-09','SB-1-STF-003','MONTHLY','2026-09-30',3,1,1,1,3),(5,'2026-09-23 16:38:18.886293','2026-09-23 17:11:43.062290',2,'2026-09-23 17:11:43.035738','This Not True','2026-09-23 16:38:18.943178','REJECTED',40000.00,'Monthly savings for 2026-09','SB-1-STF-002','MONTHLY','2026-09-30',3,1,1,1,2),(6,'2026-09-23 17:10:46.786032','2026-09-23 17:11:52.378041',2,'2026-09-23 17:11:52.353825','Okey','2026-09-23 17:10:46.890348','APPROVED',50000.00,'Monthly savings for 2026-09','SB-2-VFR01','MONTHLY','2026-09-30',3,1,1,2,1),(7,'2026-09-23 17:10:46.791640','2026-09-23 17:11:52.380060',2,'2026-09-23 17:11:52.353825','Okey','2026-09-23 17:10:46.890348','APPROVED',30000.00,'Monthly savings for 2026-09','SB-2-STF-005','MONTHLY','2026-09-30',3,1,1,2,5),(8,'2026-09-23 17:10:46.807682','2026-09-23 17:11:52.380060',2,'2026-09-23 17:11:52.353825','Okey','2026-09-23 17:10:46.890348','APPROVED',60000.00,'Monthly savings for 2026-09','SB-2-STF-004','MONTHLY','2026-09-30',3,1,1,2,4),(9,'2026-09-23 17:10:46.815054','2026-09-23 17:11:52.380060',2,'2026-09-23 17:11:52.353825','Okey','2026-09-23 17:10:46.890348','APPROVED',50000.00,'Monthly savings for 2026-09','SB-2-STF-003','MONTHLY','2026-09-30',3,1,1,2,3),(10,'2026-09-23 17:10:46.820842','2026-09-23 17:11:52.380060',2,'2026-09-23 17:11:52.353825','Okey','2026-09-23 17:10:46.890348','APPROVED',10000.00,'Monthly savings for 2026-09','SB-2-STF-006','MONTHLY','2026-09-30',3,1,1,2,6),(11,'2026-09-23 17:10:46.831103','2026-09-23 17:11:52.380060',2,'2026-09-23 17:11:52.353825','Okey','2026-09-23 17:10:46.890348','APPROVED',40000.00,'Monthly savings for 2026-09','SB-2-STF-002','MONTHLY','2026-09-30',3,1,1,2,2),(12,'2026-09-23 17:13:43.185839','2026-09-23 17:14:01.604264',2,'2026-09-23 17:14:01.600814','Okey','2026-09-23 17:13:43.245511','APPROVED',500.00,'Income of savbing','Income of savbing','INDIVIDUAL','2026-09-23',3,2,2,NULL,1),(13,'2026-09-23 17:15:09.135959','2026-09-23 17:15:50.442042',2,'2026-09-23 17:15:50.429712','Okey','2026-09-23 17:15:09.183820','APPROVED',1000.00,'This tes ','Penelities','WITHDRAWAL','2026-09-23',3,2,2,NULL,1);
/*!40000 ALTER TABLE `savings_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_settings`
--

DROP TABLE IF EXISTS `system_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `description` varchar(250) NOT NULL,
  `setting_key` varchar(100) NOT NULL,
  `setting_value` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnm18l4pyovtvd8y3b3x0l2y64` (`setting_key`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_settings`
--

LOCK TABLES `system_settings` WRITE;
/*!40000 ALTER TABLE `system_settings` DISABLE KEYS */;
INSERT INTO `system_settings` VALUES (1,'2026-09-22 22:35:24.151088','2026-09-22 22:35:24.151088',0,'Currency used in the application','currency','RWF'),(2,'2026-09-22 22:35:24.154089','2026-09-22 23:30:53.163902',1,'Default annual flat loan interest rate','defaultLoanInterestRate','16'),(3,'2026-09-22 22:35:24.155087','2026-09-22 22:35:24.155087',0,'Minimum balance allowed after a withdrawal','minimumSavingsBalance','0.00'),(4,'2026-09-22 22:35:24.157088','2026-09-22 22:35:24.157088',0,'Whether members who left may receive a new loan','allowLoansForLeftMembers','false');
/*!40000 ALTER TABLE `system_settings` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-23 21:50:37
