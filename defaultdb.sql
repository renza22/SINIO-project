-- MySQL dump 10.13  Distrib 8.0.19, for Win64 (x86_64)
--
-- Host: sinio-rendy221205-2e83.g.aivencloud.com    Database: defaultdb
-- ------------------------------------------------------
-- Server version	8.0.35

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
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'ca91d90c-b559-11f0-ae38-862ccfb06803:1-513,
d3d4a0c9-c4ff-11f0-a144-862ccfb0473f:1-2954';

--
-- Table structure for table `fasilitas`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fasilitas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKoscebgeababmix40y1wlfou0h` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fasilitas`
--

LOCK TABLES `fasilitas` WRITE;
/*!40000 ALTER TABLE `fasilitas` DISABLE KEYS */;
INSERT INTO `fasilitas` VALUES (3,'AC'),(19,'Air Minum dan Teh'),(29,'Balkon'),(11,'Bathtub'),(8,'Bathtub & shower terpisah'),(5,'Kamar mandi dalam + shower air panas'),(21,'Kulkas'),(10,'Living room'),(9,'Pemandangan kota/pantai'),(7,'Ruang makan'),(20,'Ruang Tamu Extra Luas'),(2,'TV LED kabel'),(1,'Wi-Fi berkecepatan tinggi');
/*!40000 ALTER TABLE `fasilitas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `kamar_fasilitas`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kamar_fasilitas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `facility_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5jxjmqmap2qrla77n3av3qf7n` (`facility_id`),
  KEY `FKfd2ds1gss8bvf86ou9obs0y39` (`room_id`),
  CONSTRAINT `FK5jxjmqmap2qrla77n3av3qf7n` FOREIGN KEY (`facility_id`) REFERENCES `fasilitas` (`id`),
  CONSTRAINT `FKfd2ds1gss8bvf86ou9obs0y39` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=254 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `kamar_fasilitas`
--

LOCK TABLES `kamar_fasilitas` WRITE;
/*!40000 ALTER TABLE `kamar_fasilitas` DISABLE KEYS */;
INSERT INTO `kamar_fasilitas` VALUES (46,3,33),(49,11,33),(50,8,33),(51,5,33),(52,10,33),(53,7,33),(54,9,33),(56,2,33),(57,1,33),(189,3,46),(190,19,46),(191,21,46),(192,2,46),(193,11,46),(194,8,46),(195,9,46),(196,3,47),(197,20,47),(198,1,47),(199,11,47),(200,19,47),(201,5,47),(202,3,48),(203,19,48),(204,10,48),(205,20,48),(206,1,48),(207,2,48),(208,7,48),(209,5,48),(210,8,48),(211,21,48),(216,2,50),(217,5,50),(218,3,50),(219,3,51),(220,19,51),(221,5,51),(222,2,51),(223,1,51),(224,9,51),(225,21,51),(226,7,51),(227,20,51),(228,3,52),(229,5,52),(230,7,52),(231,20,52),(232,19,52),(233,8,52),(234,21,52),(235,11,52),(236,10,52),(237,2,52),(238,1,52),(239,9,52);
/*!40000 ALTER TABLE `kamar_fasilitas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `kamar_tipe`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kamar_tipe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `tipe_kamar_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_id` (`room_id`),
  KEY `fk_kamar_tipe_type` (`tipe_kamar_id`),
  CONSTRAINT `fk_kamar_tipe_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_kamar_tipe_type` FOREIGN KEY (`tipe_kamar_id`) REFERENCES `tipe_kamar` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `kamar_tipe`
--

LOCK TABLES `kamar_tipe` WRITE;
/*!40000 ALTER TABLE `kamar_tipe` DISABLE KEYS */;
INSERT INTO `kamar_tipe` VALUES (2,33,3),(5,41,3),(6,42,4),(7,43,5),(9,45,3),(10,46,6),(11,47,7),(12,48,8),(13,49,9),(14,50,11),(15,51,12),(16,52,10);
/*!40000 ALTER TABLE `kamar_tipe` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `karyawan`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `karyawan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6acbfmrk1el2fl71vngf7qw9j` (`user_id`),
  CONSTRAINT `FKa6j48s4khcxlx7lqb2sd3a56t` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `karyawan`
--

LOCK TABLES `karyawan` WRITE;
/*!40000 ALTER TABLE `karyawan` DISABLE KEYS */;
INSERT INTO `karyawan` VALUES (2,17),(1,19),(11,33);
/*!40000 ALTER TABLE `karyawan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `karyawan_roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `karyawan_roles` (
  `karyawan_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`karyawan_id`,`role_id`),
  KEY `FKd0fdldypplbeik2n96877ucs` (`role_id`),
  CONSTRAINT `FKd0fdldypplbeik2n96877ucs` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKiumf3ein44q2cv6cm6no9jabi` FOREIGN KEY (`karyawan_id`) REFERENCES `karyawan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `karyawan_roles`
--

LOCK TABLES `karyawan_roles` WRITE;
/*!40000 ALTER TABLE `karyawan_roles` DISABLE KEYS */;
INSERT INTO `karyawan_roles` VALUES (1,1),(2,1),(11,3);
/*!40000 ALTER TABLE `karyawan_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(12,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `order_id` varchar(100) NOT NULL,
  `payment_type` varchar(50) DEFAULT NULL,
  `snap_token` varchar(500) DEFAULT NULL,
  `status` enum('CANCELLED','EXPIRED','FAILED','PENDING','SUCCESS') NOT NULL,
  `transaction_id` varchar(100) DEFAULT NULL,
  `transaction_time` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `reservation_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8vo36cen604as7etdfwmyjsxt` (`order_id`),
  KEY `FKp8yh4sjt3u0g6aru1oxfh3o14` (`reservation_id`),
  CONSTRAINT `FKp8yh4sjt3u0g6aru1oxfh3o14` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
INSERT INTO `payments` VALUES (35,200000.00,'2025-11-28 18:54:08.076941','ORD-77-20251128185407','MIDTRANS','837f7409-0d7d-4bb7-acf2-d7082a152bb9','SUCCESS',NULL,'2025-11-28 18:54:08.076941','2025-11-28 18:55:01.849748',77),(38,225000.00,'2025-11-28 20:57:32.260746','ORD-80-20251128205732','CASH',NULL,'SUCCESS',NULL,'2025-11-28 20:57:32.259915','2025-11-28 20:57:32.260746',80),(49,200000.00,'2025-11-29 20:44:42.485879','ORD-92-20251129204442','CASH',NULL,'SUCCESS',NULL,'2025-11-29 20:44:42.483846','2025-11-29 20:49:23.489673',92),(51,300000.00,'2025-11-29 20:58:48.843441','ORD-94-20251129205848','CASH',NULL,'SUCCESS',NULL,'2025-11-29 20:58:48.839987','2025-11-29 21:02:46.232133',94),(54,200000.00,'2025-11-29 21:48:01.238103','ORD-97-20251129214801','CASH',NULL,'SUCCESS',NULL,'2025-11-29 21:48:01.238103','2025-11-29 23:47:27.795163',97),(55,200000.00,'2025-11-29 23:46:36.514279','ORD-98-20251129234636','CASH',NULL,'SUCCESS',NULL,'2025-11-29 23:46:36.513282','2025-11-29 23:47:24.109540',98),(58,300000.00,'2025-11-30 12:07:59.761394','ORD-103-20251130120759','CASH',NULL,'SUCCESS',NULL,'2025-11-30 12:07:59.760448','2025-11-30 12:09:07.170164',103),(59,505000.00,'2025-11-30 12:17:17.440644','ORD-104-20251130121716','MIDTRANS','2e4d67e8-0330-4650-90f2-e602f8820e60','SUCCESS',NULL,'2025-11-30 12:17:17.439476','2025-11-30 12:17:51.785175',104),(60,520000.00,'2025-11-30 12:20:27.033689','ORD-105-20251130122026','MIDTRANS','65ec4775-9acd-4938-bc54-8f45d466983e','SUCCESS',NULL,'2025-11-30 12:20:27.032692','2025-11-30 12:22:03.199092',105),(61,240000.00,'2025-11-30 19:37:17.173601','ORD-107-20251130193712','MIDTRANS','2fd19364-e132-4035-9318-b1057407471b','SUCCESS',NULL,'2025-11-30 19:37:17.169614','2025-11-30 19:39:00.602027',107),(62,240000.00,'2025-11-30 19:41:11.159940','ORD-106-20251130194109','MIDTRANS','9da42d90-d482-448b-ad93-c686cf37df54','SUCCESS',NULL,'2025-11-30 19:41:11.152857','2025-11-30 19:49:48.384679',106),(63,300000.00,'2025-12-01 07:02:36.473424','ORD-108-20251201070236','CASH',NULL,'SUCCESS',NULL,'2025-12-01 07:02:36.472573','2025-12-01 07:03:58.564400',108),(64,200000.00,'2025-12-01 07:42:09.701561','ORD-109-20251201074209','MIDTRANS','718cce0b-79e7-45b3-8923-5e4303ef34c3','SUCCESS',NULL,'2025-12-01 07:42:09.701142','2025-12-01 07:42:27.787615',109),(74,260000.00,'2025-12-03 13:11:18.373401','ORD-119-20251203131117','MIDTRANS','120b61ed-3f10-430d-9fed-4ec8bb8ac8d5','CANCELLED',NULL,'2025-12-03 13:11:18.373401','2025-12-03 13:54:40.589678',119),(75,450000.00,'2025-12-03 13:58:48.006741','ORD-120-20251203135847','MIDTRANS','e902cf66-19fa-44ef-8730-04f9146f360a','CANCELLED',NULL,'2025-12-03 13:58:48.005322','2025-12-03 14:02:54.009039',120),(76,1140000.00,'2025-12-03 14:05:02.723489','ORD-121-20251203140721','MIDTRANS','ae9738e2-af93-4da8-bd6f-c57000f7cfc2','CANCELLED',NULL,'2025-12-03 14:07:22.398880','2025-12-03 14:08:01.869427',121),(77,175000.00,'2025-12-03 14:08:38.785028','ORD-122-20251203140838','MIDTRANS','1344351c-327a-4867-8aa8-d8b814c10c28','SUCCESS',NULL,'2025-12-03 14:08:38.785028','2025-12-03 14:09:15.381518',122),(78,240000.00,'2025-12-03 14:45:13.959242','ORD-123-20251203144525','MIDTRANS','a31ff3a8-1d9d-4868-a84e-b8079e157c1f','SUCCESS',NULL,'2025-12-03 14:45:25.939777','2025-12-03 14:46:03.407430',123),(79,1720000.00,'2025-12-03 21:40:01.190286','ORD-124-20251204044144','MIDTRANS','e9049b4c-77f9-4a72-992b-cc37a0d7e395','CANCELLED',NULL,'2025-12-04 04:41:45.444775','2025-12-04 04:42:39.275084',124),(80,370000.00,'2025-12-04 04:51:10.915860','ORD-125-20251204045110','MIDTRANS','a2017443-1daf-4fa0-9c2c-927e623cab67','CANCELLED',NULL,'2025-12-04 04:51:10.914859','2025-12-04 04:56:07.831724',125),(81,1500000.00,'2025-12-04 04:53:24.931448','ORD-126-20251204045324','MIDTRANS','38199d03-a0df-455b-973f-1cc83d94335e','CANCELLED',NULL,'2025-12-04 04:53:24.930817','2025-12-04 04:54:12.173218',126),(82,800000.00,'2025-12-04 04:54:58.932303','ORD-127-20251204045458','MIDTRANS','bf29e2a1-b9c3-4b1d-bf43-7785cf3bac18','CANCELLED',NULL,'2025-12-04 04:54:58.931301','2025-12-04 04:56:02.087932',127),(83,1540000.00,'2025-12-04 04:59:59.639640','ORD-128-20251204050321','MIDTRANS','99591327-bbd5-44cf-abe5-a03877021bd3','CANCELLED',NULL,'2025-12-04 05:03:22.364702','2025-12-04 05:06:24.754550',128),(84,1500000.00,'2025-12-04 05:09:19.565772','ORD-129-20251204050938','MIDTRANS','eaaa91bc-5700-4c2d-acb6-501f2f81b755','SUCCESS',NULL,'2025-12-04 05:09:38.434772','2025-12-04 05:10:33.818935',129),(85,1540000.00,'2025-12-04 05:14:51.023168','ORD-130-20251204051450','MIDTRANS','d075a0d2-7733-44a1-9c15-403d7510a264','CANCELLED',NULL,'2025-12-04 05:14:51.020642','2025-12-04 05:16:01.139004',130),(86,425000.00,'2025-12-04 06:31:52.771875','ORD-133-20251204063152','MIDTRANS','f837ea4d-7757-4678-945b-aa2a8c44b68c','CANCELLED',NULL,'2025-12-04 06:31:52.770296','2025-12-04 06:32:55.300470',133),(87,220000.00,'2025-12-04 01:11:12.514158','ORD-134-20251204011131','MIDTRANS','52002577-45fb-491f-9fd8-b56b46c36c79','SUCCESS',NULL,'2025-12-04 01:11:32.010427','2025-12-04 01:12:06.083641',134),(88,625000.00,'2025-12-04 01:19:07.222134','ORD-135-20251204011907','MIDTRANS','01566598-ab7c-47f4-93c5-06fed279959c','SUCCESS',NULL,'2025-12-04 01:19:07.221626','2025-12-04 01:19:38.795204',135),(89,400000.00,'2025-12-04 03:26:42.696692','ORD-137-20251204032642','MIDTRANS','1c233667-b158-4380-a074-56c7f4eb8a34','SUCCESS',NULL,'2025-12-04 03:26:42.696350','2025-12-04 03:27:31.233300',137),(90,3170000.00,'2025-12-08 12:57:39.954851','ORD-138-20251208125739','CASH',NULL,'SUCCESS',NULL,'2025-12-08 12:57:39.954439','2025-12-08 13:00:13.094296',138);
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservation_rooms`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation_rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nightly_rate` decimal(12,2) NOT NULL,
  `reservation_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK96vqam6q3l5mic9bf9unp1q9j` (`reservation_id`),
  KEY `FK81imhuuxynjj2ivy82koy38nm` (`room_id`),
  CONSTRAINT `FK81imhuuxynjj2ivy82koy38nm` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FK96vqam6q3l5mic9bf9unp1q9j` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=123 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservation_rooms`
--

LOCK TABLES `reservation_rooms` WRITE;
/*!40000 ALTER TABLE `reservation_rooms` DISABLE KEYS */;
INSERT INTO `reservation_rooms` VALUES (76,200000.00,92,42),(78,300000.00,94,41),(81,200000.00,97,42),(82,200000.00,98,42),(87,300000.00,103,41),(88,200000.00,104,42),(89,300000.00,105,41),(90,200000.00,107,42),(91,200000.00,106,42),(92,300000.00,108,41),(93,200000.00,109,42),(103,200000.00,119,42),(104,350000.00,120,41),(105,1100000.00,121,51),(106,150000.00,122,43),(107,200000.00,123,42),(108,1500000.00,124,52),(109,350000.00,125,41),(110,1500000.00,126,52),(111,800000.00,127,49),(112,1500000.00,128,52),(113,1500000.00,129,52),(114,1500000.00,130,52),(116,120000.00,132,45),(117,400000.00,133,50),(118,200000.00,134,42),(119,600000.00,135,47),(120,1500000.00,136,52),(121,200000.00,137,42),(122,1500000.00,138,52);
/*!40000 ALTER TABLE `reservation_rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservation_services`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation_services` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `service_name` varchar(120) NOT NULL,
  `service_unit` varchar(32) DEFAULT NULL,
  `service_price` decimal(12,2) DEFAULT NULL,
  `reservation_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKt6smgnpps5u0771p2qyefc25m` (`reservation_id`),
  CONSTRAINT `FKt6smgnpps5u0771p2qyefc25m` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=164 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservation_services`
--

LOCK TABLES `reservation_services` WRITE;
/*!40000 ALTER TABLE `reservation_services` DISABLE KEYS */;
INSERT INTO `reservation_services` VALUES (113,'Laundry','kg',25000.00,80),(119,'Laundry','kg',25000.00,104),(120,'Room dining','porsi',60000.00,104),(121,'Spa (60 menit)','sesi',180000.00,104),(122,'Pembersihan ekstra','kali',40000.00,104),(123,'Laundry','kg',20000.00,105),(124,'SPA','1',50000.00,105),(125,'Sauna','1',50000.00,105),(126,'transportasi','1',100000.00,105),(127,'Pembersihan ekstra','kali',40000.00,106),(128,'Pembersihan ekstra','kali',40000.00,107),(138,'Room dining','porsi',60000.00,119),(139,'SPA','1',50000.00,120),(140,'Sauna','1',50000.00,120),(141,'Pembersihan ekstra','kali',40000.00,121),(142,'Laundry','kg',25000.00,122),(143,'Pembersihan ekstra','kali',40000.00,123),(144,'Spa (60 menit)','sesi',180000.00,124),(145,'Pembersihan ekstra','kali',40000.00,124),(146,'Sewa Baby Crib','1',20000.00,125),(147,'Pembersihan ekstra','kali',40000.00,128),(148,'Pembersihan ekstra','kali',40000.00,130),(149,'Camilan Buah','2',25000.00,131),(150,'Tour','1',35000.00,131),(151,'Laundry','kg',25000.00,133),(152,'Sewa Baby Crib','unit',20000.00,134),(153,'Laundry','kg',25000.00,135),(154,'Laundry','kg',25000.00,138),(155,'Room dining','porsi',60000.00,138),(156,'Pembersihan ekstra','kali',40000.00,138),(157,'Camilan Buah','porsi',25000.00,138),(158,'Pick-up service','trip',50000.00,138),(159,'Sewa Baby Crib','unit',20000.00,138),(160,'SPA','sesi',500000.00,138),(161,'Sauna','sesi',500000.00,138),(162,'Transportasi','trip',100000.00,138),(163,'Tour','paket',350000.00,138);
/*!40000 ALTER TABLE `reservation_services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_in` date NOT NULL,
  `check_out` date NOT NULL,
  `code` varchar(32) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `status` enum('BOOKED','CANCELED','CHECKED_IN','CHECKED_OUT','CONFIRMED','PENDING_PAYMENT') NOT NULL,
  `room_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKfy8tg3s0aqt56jpagw2djmx4w` (`code`),
  KEY `FKljt6q1tp205b0h26eiegc5mx6` (`room_id`),
  KEY `FKb5g9io5h54iwl2inkno50ppln` (`user_id`),
  CONSTRAINT `FKb5g9io5h54iwl2inkno50ppln` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKljt6q1tp205b0h26eiegc5mx6` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=139 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservations`
--

LOCK TABLES `reservations` WRITE;
/*!40000 ALTER TABLE `reservations` DISABLE KEYS */;
INSERT INTO `reservations` VALUES (77,'2025-11-28','2025-11-29','RSV-77','2025-11-28 18:54:06.165397','CHECKED_OUT',33,24),(80,'2025-11-28','2025-11-29','RSV-80','2025-11-28 20:57:09.557682','CHECKED_OUT',33,24),(92,'2025-11-29','2025-11-30','RSV-92','2025-11-29 20:43:58.206289','CHECKED_OUT',42,24),(94,'2025-11-29','2025-11-30','RSV-94','2025-11-29 20:58:47.009457','CHECKED_OUT',41,24),(97,'2025-11-29','2025-11-30','RSV-97','2025-11-29 21:47:59.429482','CHECKED_OUT',42,24),(98,'2025-11-29','2025-11-30','RSV-98','2025-11-29 23:46:29.477570','CHECKED_OUT',42,24),(103,'2025-11-30','2025-12-01','RSV-103','2025-11-30 12:07:07.445613','CHECKED_OUT',41,24),(104,'2025-11-30','2025-12-01','RSV-104','2025-11-30 12:13:01.666958','CHECKED_OUT',42,24),(105,'2025-11-30','2025-12-01','RSV-105','2025-11-30 12:20:23.182555','CHECKED_OUT',41,24),(106,'2025-11-30','2025-12-01','RSV-106','2025-11-30 19:36:35.111211','CHECKED_OUT',42,24),(107,'2025-11-30','2025-12-01','RSV-107','2025-11-30 19:36:36.006177','CHECKED_OUT',42,24),(108,'2025-12-01','2025-12-02','RSV-108','2025-12-01 07:02:27.313296','CHECKED_OUT',41,24),(109,'2025-12-01','2025-12-02','RSV-109','2025-12-01 07:41:51.111508','CHECKED_OUT',42,25),(119,'2025-12-03','2025-12-04','RSV-119','2025-12-03 13:11:12.715953','CANCELED',42,24),(120,'2025-12-03','2025-12-04','RSV-120','2025-12-03 13:58:40.920926','CANCELED',41,24),(121,'2025-12-03','2025-12-04','RSV-121','2025-12-03 14:04:52.985652','CANCELED',51,24),(122,'2025-12-03','2025-12-04','RSV-122','2025-12-03 14:08:33.509093','CHECKED_OUT',43,24),(123,'2025-12-03','2025-12-04','RSV-123','2025-12-03 14:45:10.048471','CHECKED_OUT',42,24),(124,'2025-12-04','2025-12-05','RSV-124','2025-12-03 21:39:54.735979','CANCELED',52,35),(125,'2025-12-04','2025-12-05','RSV-125','2025-12-04 04:51:07.269879','CANCELED',41,35),(126,'2025-12-04','2025-12-05','RSV-126','2025-12-04 04:53:21.691402','CANCELED',52,35),(127,'2025-12-04','2025-12-05','RSV-127','2025-12-04 04:54:55.320357','CANCELED',49,35),(128,'2025-12-04','2025-12-05','RSV-128','2025-12-04 04:59:55.975775','CANCELED',52,35),(129,'2025-12-04','2025-12-05','RSV-129','2025-12-04 05:06:45.156002','CHECKED_OUT',52,35),(130,'2025-12-04','2025-12-05','RSV-130','2025-12-04 05:09:13.004096','CANCELED',52,35),(131,'2025-12-04','2025-12-05','RSV-131','2025-12-04 05:41:12.757294','CANCELED',41,36),(132,'2025-12-04','2025-12-05','RSV-132','2025-12-04 05:51:35.065164','CHECKED_OUT',45,36),(133,'2025-12-04','2025-12-05','RSV-133','2025-12-04 06:31:38.162244','CANCELED',50,35),(134,'2025-12-03','2025-12-04','RSV-134','2025-12-04 01:11:07.690095','CHECKED_OUT',42,40),(135,'2025-12-04','2025-12-05','RSV-135','2025-12-04 01:19:02.729235','CHECKED_OUT',47,24),(136,'2025-12-04','2025-12-05','RSV-136','2025-12-04 01:24:35.894654','CHECKED_OUT',52,41),(137,'2025-12-05','2025-12-07','RSV-137','2025-12-04 03:26:29.563544','BOOKED',42,26),(138,'2025-12-08','2025-12-09','RSV-138','2025-12-08 12:57:08.629247','CONFIRMED',52,42);
/*!40000 ALTER TABLE `reservations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(60) NOT NULL,
  `name` varchar(120) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKch1113horj4qr56f91omojv8` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'KASIR','Kasir'),(2,'HOUSEKEEPING','Housekeeping'),(3,'RESEPSIONIS','Resepsionis');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_amenities`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_amenities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `sort_order` int DEFAULT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKps6ofup9gxhn8juqvproxbaud` (`room_id`),
  CONSTRAINT `FKps6ofup9gxhn8juqvproxbaud` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=307 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_amenities`
--

LOCK TABLES `room_amenities` WRITE;
/*!40000 ALTER TABLE `room_amenities` DISABLE KEYS */;
INSERT INTO `room_amenities` VALUES (86,'AC',0,33),(87,'AC\\nKamar mandi dalam + shower air panas\\nWi-Fi berkecepatan tinggi\\nTV LED kabel',1,33),(88,'Air minum & teh/kopi',2,33),(89,'Bathtub',3,33),(90,'Bathtub & shower terpisah',4,33),(91,'Kamar mandi dalam + shower air panas',5,33),(92,'Living room',6,33),(93,'Ruang makan',7,33),(94,'Pemandangan kota/pantai',8,33),(95,'Ruang tamu luas',9,33),(96,'TV LED kabel',10,33),(97,'Wi-Fi berkecepatan tinggi',11,33),(98,'Wi-Fi berkecepatan tinggi\\nTV LED kabel\\nAC\\nAir minum & teh/kopi\\nKamar mandi dalam + shower air panas',12,33),(230,'AC\\nAC\\nKamar mandi dalam + shower air panas\\nWi-Fi berkecepatan tinggi\\nTV LED kabel\\nAir minum & teh/kopi\\nBathtub & shower te',0,42),(231,'AC\\nBathtub & shower terpisah\\nAir minum & teh/kopi\\nBathtub\\nKamar mandi dalam + shower air panas\\nPemandangan kota/pantai\\nRua',0,43),(232,'AC',0,46),(233,'Air Minum dan Teh',1,46),(234,'Kulkas',2,46),(235,'TV LED kabel',3,46),(236,'Bathtub',4,46),(237,'Bathtub & shower terpisah',5,46),(238,'Pemandangan kota/pantai',6,46),(239,'AC',0,47),(240,'Ruang Tamu Extra Luas',1,47),(241,'Wi-Fi berkecepatan tinggi',2,47),(242,'Bathtub',3,47),(243,'Air Minum dan Teh',4,47),(244,'Kamar mandi dalam + shower air panas',5,47),(245,'AC',0,48),(246,'Air Minum dan Teh',1,48),(247,'Living room',2,48),(248,'Ruang Tamu Extra Luas',3,48),(249,'Wi-Fi berkecepatan tinggi',4,48),(250,'TV LED kabel',5,48),(251,'Ruang makan',6,48),(252,'Kamar mandi dalam + shower air panas',7,48),(253,'Bathtub & shower terpisah',8,48),(254,'Kulkas',9,48),(259,'TV LED kabel',0,50),(260,'Kamar mandi dalam + shower air panas',1,50),(261,'AC',2,50),(262,'AC',0,51),(263,'Air Minum dan Teh',1,51),(264,'Kamar mandi dalam + shower air panas',2,51),(265,'TV LED kabel',3,51),(266,'Wi-Fi berkecepatan tinggi',4,51),(267,'Pemandangan kota/pantai',5,51),(268,'Kulkas',6,51),(269,'Ruang makan',7,51),(270,'Ruang Tamu Extra Luas',8,51),(271,'AC',0,52),(272,'Kamar mandi dalam + shower air panas',1,52),(273,'Ruang makan',2,52),(274,'Ruang Tamu Extra Luas',3,52),(275,'Air Minum dan Teh',4,52),(276,'Bathtub & shower terpisah',5,52),(277,'Kulkas',6,52),(278,'Bathtub',7,52),(279,'Living room',8,52),(280,'TV LED kabel',9,52),(281,'Wi-Fi berkecepatan tinggi',10,52),(282,'Pemandangan kota/pantai',11,52),(284,'AC\\nWi-Fi berkecepatan tinggi\\nRuang Tamu Extra Luas\\nKamar mandi dalam + shower air panas',0,49),(290,'Bathtub\\nLiving room',0,41),(300,'Kamar mandi dalam + shower air panas\\nBathtub & shower terpisah\\nWi-Fi berkecepatan tinggi\\nTV LED kabel\\nAC\\nAir minum & teh/ko',0,45);
/*!40000 ALTER TABLE `room_amenities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_images`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sort_order` int DEFAULT NULL,
  `url` varchar(500) NOT NULL,
  `room_id` bigint NOT NULL,
  `delete_url` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKtky1jnwoh1hv50m263p2vlt0y` (`room_id`),
  CONSTRAINT `FKtky1jnwoh1hv50m263p2vlt0y` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_images`
--

LOCK TABLES `room_images` WRITE;
/*!40000 ALTER TABLE `room_images` DISABLE KEYS */;
INSERT INTO `room_images` VALUES (18,0,'https://i.ibb.co/ZRNZVzCy/deluxe-twin1.png',42,'https://ibb.co/TDrCkM5z/cd0a9cc526d633c072996c9c5b137ec7'),(19,1,'https://i.ibb.co/xK1W2wYv/deluxe-twin3.png',42,'https://ibb.co/7xnPSBQc/15f9e7fcb5077c173cee5bd823e0bc85'),(20,2,'https://i.ibb.co/MxFrhP67/deluxe-twin2.png',42,'https://ibb.co/YFHmWfZh/30c361a2107cb687f4848250509df917'),(21,0,'https://i.ibb.co/m7F0TkQ/Suite-Panorama1.png',43,'https://ibb.co/JPFqtVX/83ff629e261e1fe3e4a1f2655b437481'),(22,1,'https://i.ibb.co/FkJGYk6x/Suite-Panorama3.png',43,'https://ibb.co/0pZP9pqD/09a253013f45606a7898e562b391a90d'),(23,2,'https://i.ibb.co/C5MX9DvX/Suite-Panorama2.png',43,'https://ibb.co/nsnV1tfV/9518838108aade3e641f7ceb10bdebd2'),(24,0,'https://i.ibb.co/4gmND4bK/Superior-Twin1.png',46,'https://ibb.co/JFBkbrYm/4ad93a7212ba5a5999874ea07f386bf0'),(25,1,'https://i.ibb.co/n8shRScv/Superior-Twin3.png',46,'https://ibb.co/sJvLtrwG/111bb914b5e65ce0f1e2a828ffbd638e'),(26,2,'https://i.ibb.co/C3bp86wx/Superior-Twin2.png',46,'https://ibb.co/pBf6wQ1C/fdc98ba3c5801a934e7b51e178dd30f0'),(27,0,'https://i.ibb.co/BKPqYNxY/Studio-Loft-2.png',47,'https://ibb.co/YTWpVZMV/b9eb2d7b0c0183c6e3f586e8d2aa5615'),(28,1,'https://i.ibb.co/VYH5L2ZW/Studio-Loft-3.png',47,'https://ibb.co/b5HD6zZg/1d3beb504c79c900452a08748aa79e7d'),(29,2,'https://i.ibb.co/qFXSdJ02/Studio-Loft-1.png',47,'https://ibb.co/93zDNZwd/46f453445675ff24451f2a1cda66ed4e'),(30,0,'https://i.ibb.co/1fycLb8B/Executive-Suite-1.png',48,'https://ibb.co/tMjR438n/79826fd00086b4e00c3282d1dff23ef7'),(31,1,'https://i.ibb.co/1JX0Nkxc/Executive-Suite-2.png',48,'https://ibb.co/DgM1FZnd/2855c4f10c156f82bf4df84463690e67'),(32,2,'https://i.ibb.co/v6C6Nttz/Executive-Suite-3.png',48,'https://ibb.co/HfLfcJJg/d143429f4e2a319766fc0f7c4372e9f8'),(36,0,'https://i.ibb.co/rRZt5qRb/Standar1.png',50,'https://ibb.co/21j6cR18/81f9c4a3bd5497eaff1e6062d44bf41b'),(37,1,'https://i.ibb.co/KjBZTthm/Standar2.png',50,'https://ibb.co/wN8H5ngY/2e28f3c62054c7cbd17105fe18599b77'),(38,2,'https://i.ibb.co/hJQHgQwd/Standar3.png',50,'https://ibb.co/DHck1cTM/e32a85aaec783877f7479a2fcabce439'),(39,0,'https://i.ibb.co/wF07FN0L/vila1.png',51,'https://ibb.co/G4x94vxJ/d40f2c699bbeb4049af400d7e910e015'),(40,1,'https://i.ibb.co/0jC4ZGBB/vila2.png',51,'https://ibb.co/mC9Ztcvv/4dc1f955703db6604a4d78b62ac2cad5'),(41,2,'https://i.ibb.co/vvCYM0RR/vila3.png',51,'https://ibb.co/Z1Rmbq77/d4a551f892450c0325c9bef1f6a142a5'),(42,0,'https://i.ibb.co/zH7PMq0q/presi.jpg',52,'https://ibb.co/h1Z263t3/db58d1634a6ee97e7059777dda324e44'),(43,1,'https://i.ibb.co/PzDT5Yrf/pres.jpg',52,'https://ibb.co/TqWKHTwF/c5596db27eb8c502fae0e26eece7d5d0'),(44,2,'https://i.ibb.co/Q3dwz8Ym/pre.jpg',52,'https://ibb.co/RkywZhQH/c50b1b6a694c8157cfa32d62ad897339'),(46,0,'https://i.ibb.co/h1x5LH47/Family-Room1.png',49,'https://ibb.co/NnghtpJV/616b6f7c87880c9f40f7764e1496efd8'),(47,1,'https://i.ibb.co/GvxxH4bg/Family-Room2.png',49,'https://ibb.co/kgHHmsWY/3b2039f5b3e530270a8c3cb25c4160b8'),(48,2,'https://i.ibb.co/d4t2qg85/Family-Room3.png',49,'https://ibb.co/F4XwMVRD/6d26e72f83d404fe94a6ee82923e1436'),(52,0,'https://i.ibb.co/jZrr53VR/Deluxe-King1.png',41,'https://ibb.co/wrzzLpJs/d0252a0def98f9ddd2126de4480e3848\\nhttps://i.ibb.co/twTqp6bY/Deluxe-King3.png'),(59,0,'https://i.ibb.co/SwxRSMVJ/adsdasdsd.jpg',45,'\\nhttps://i.ibb.co/KxpJ49yQ/asdsadsds.jpg');
/*!40000 ALTER TABLE `room_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_service_options`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_service_options` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `sort_order` int DEFAULT NULL,
  `unit` varchar(32) NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm762gar3yxwe4quhyvd78kq28` (`room_id`),
  CONSTRAINT `FKm762gar3yxwe4quhyvd78kq28` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=223 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_service_options`
--

LOCK TABLES `room_service_options` WRITE;
/*!40000 ALTER TABLE `room_service_options` DISABLE KEYS */;
INSERT INTO `room_service_options` VALUES (62,'Laundry',25000.00,0,'kg',33),(63,'Room dining',60000.00,1,'porsi',33),(64,'Spa (60 menit)',180000.00,2,'sesi',33),(65,'Pembersihan ekstra',40000.00,3,'kali',33),(146,'Laundry',25000.00,0,'kg',41),(147,'Room dining',60000.00,1,'porsi',41),(148,'Pembersihan ekstra',40000.00,2,'kali',41),(149,'Camilan Buah',25000.00,3,'porsi',41),(150,'Pick-up service',50000.00,4,'trip',41),(151,'Sewa Baby Crib',20000.00,5,'unit',41),(152,'Laundry',25000.00,0,'kg',45),(153,'Room dining',60000.00,1,'porsi',45),(154,'Pembersihan ekstra',40000.00,2,'kali',45),(155,'Camilan Buah',25000.00,3,'porsi',45),(156,'Pick-up service',50000.00,4,'trip',45),(157,'Sewa Baby Crib',20000.00,5,'unit',45),(158,'Laundry',25000.00,0,'kg',42),(159,'Room dining',60000.00,1,'porsi',42),(160,'Pembersihan ekstra',40000.00,2,'kali',42),(161,'Camilan Buah',25000.00,3,'porsi',42),(162,'Pick-up service',50000.00,4,'trip',42),(163,'Sewa Baby Crib',20000.00,5,'unit',42),(164,'Laundry',25000.00,0,'kg',49),(165,'Room dining',60000.00,1,'porsi',49),(166,'Pembersihan ekstra',40000.00,2,'kali',49),(167,'Camilan Buah',25000.00,3,'porsi',49),(168,'Pick-up service',50000.00,4,'trip',49),(169,'Sewa Baby Crib',20000.00,5,'unit',49),(170,'Laundry',25000.00,0,'kg',46),(171,'Room dining',60000.00,1,'porsi',46),(172,'Pembersihan ekstra',40000.00,2,'kali',46),(173,'Camilan Buah',25000.00,3,'porsi',46),(174,'Pick-up service',50000.00,4,'trip',46),(175,'Sewa Baby Crib',20000.00,5,'unit',46),(176,'Laundry',25000.00,0,'kg',47),(177,'Room dining',60000.00,1,'porsi',47),(178,'Pembersihan ekstra',40000.00,2,'kali',47),(179,'Laundry',25000.00,0,'kg',50),(180,'Room dining',60000.00,1,'porsi',50),(181,'Pembersihan ekstra',40000.00,2,'kali',50),(182,'Laundry',25000.00,0,'kg',43),(183,'Room dining',60000.00,1,'porsi',43),(184,'Pembersihan ekstra',40000.00,2,'kali',43),(185,'Camilan Buah',25000.00,3,'porsi',43),(186,'Pick-up service',50000.00,4,'trip',43),(187,'Sewa Baby Crib',20000.00,5,'unit',43),(188,'SPA',500000.00,6,'sesi',43),(189,'Sauna',500000.00,7,'sesi',43),(190,'Transportasi',100000.00,8,'trip',43),(191,'Tour',350000.00,9,'paket',43),(192,'Laundry',25000.00,0,'kg',48),(193,'Room dining',60000.00,1,'porsi',48),(194,'Pembersihan ekstra',40000.00,2,'kali',48),(195,'Camilan Buah',25000.00,3,'porsi',48),(196,'Pick-up service',50000.00,4,'trip',48),(197,'Sewa Baby Crib',20000.00,5,'unit',48),(198,'SPA',500000.00,6,'sesi',48),(199,'Sauna',500000.00,7,'sesi',48),(200,'Transportasi',100000.00,8,'trip',48),(201,'Tour',350000.00,9,'paket',48),(202,'Laundry',25000.00,0,'kg',51),(203,'Room dining',60000.00,1,'porsi',51),(204,'Pembersihan ekstra',40000.00,2,'kali',51),(205,'Camilan Buah',25000.00,3,'porsi',51),(206,'Pick-up service',50000.00,4,'trip',51),(207,'Sewa Baby Crib',20000.00,5,'unit',51),(208,'SPA',500000.00,6,'sesi',51),(209,'Sauna',500000.00,7,'sesi',51),(210,'Transportasi',100000.00,8,'trip',51),(211,'Tour',350000.00,9,'paket',51),(212,'Laundry',25000.00,0,'kg',52),(213,'Room dining',60000.00,1,'porsi',52),(214,'Pembersihan ekstra',40000.00,2,'kali',52),(215,'Camilan Buah',25000.00,3,'porsi',52),(216,'Pick-up service',50000.00,4,'trip',52),(217,'Sewa Baby Crib',20000.00,5,'unit',52),(218,'SPA',500000.00,6,'sesi',52),(219,'Sauna',500000.00,7,'sesi',52),(220,'Transportasi',100000.00,8,'trip',52),(221,'Tour',350000.00,9,'paket',52);
/*!40000 ALTER TABLE `room_service_options` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rooms`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `last_cleaned_at` datetime(6) DEFAULT NULL,
  `note` text,
  `number` varchar(16) NOT NULL,
  `rate` decimal(12,2) NOT NULL,
  `status` enum('AVAILABLE','BOOKED','CLEANING','MAINTENANCE','OCCUPIED') NOT NULL,
  `type` enum('DELUXE_KING','DELUXE_TWIN','EXECUTIVE_SUITE','FAMILY_ROOM','PRESIDENTIAL_SUITE','STANDARD','STUDIO_LOFT','SUITE_PANORAMA','SUPERIOR_TWIN','VILLA') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `max_occupancy` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdas0g3gx65rc2af4dxqgu47sy` (`number`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rooms`
--

LOCK TABLES `rooms` WRITE;
/*!40000 ALTER TABLE `rooms` DISABLE KEYS */;
INSERT INTO `rooms` VALUES (41,'2025-11-29 13:03:49.509063','2025-12-01 07:05:00.000000','mampir mas','AA1',350000.00,'AVAILABLE','DELUXE_KING','2025-12-04 05:56:24.977207',2),(42,'2025-11-29 13:04:32.729577','2025-12-04 01:16:25.561004','Selamat Datang Di Sinio','BB1',200000.00,'AVAILABLE','DELUXE_TWIN','2025-12-08 12:59:26.961141',2),(43,'2025-11-29 13:05:18.897549','2025-12-03 14:49:52.203967','mampir neng','CC1',150000.00,'AVAILABLE','SUITE_PANORAMA','2025-12-03 14:49:52.203967',2),(45,'2025-11-30 21:13:49.271804','2025-12-04 05:52:16.858688','Tekan bel untuk panggil malaikat cantik','AA2',120000.00,'AVAILABLE','DELUXE_KING','2025-12-04 05:52:16.859744',2),(46,'2025-12-02 07:53:20.608253','2025-12-02 00:00:00.000000','','DD1',500000.00,'AVAILABLE','SUPERIOR_TWIN','2025-12-02 07:53:20.608253',2),(47,'2025-12-02 07:54:39.354844','2025-12-04 01:24:44.347299','','EE1',600000.00,'AVAILABLE','STUDIO_LOFT','2025-12-04 01:24:44.347825',2),(48,'2025-12-02 07:57:07.891502','2025-12-02 00:00:00.000000','','FF1',700000.00,'AVAILABLE','EXECUTIVE_SUITE','2025-12-02 07:57:07.891502',2),(49,'2025-12-02 08:00:03.717690','2025-12-03 00:00:00.000000','','GG1',800000.00,'AVAILABLE','FAMILY_ROOM','2025-12-04 04:56:02.087932',4),(50,'2025-12-02 08:03:18.240765','2025-12-01 00:00:00.000000','','HH1',400000.00,'AVAILABLE','STANDARD','2025-12-04 06:32:55.299472',1),(51,'2025-12-02 08:05:09.033513','2025-12-12 00:00:00.000000','','ZZ1',1100000.00,'AVAILABLE','VILLA','2025-12-02 08:05:09.033513',4),(52,'2025-12-02 08:10:09.570127','2025-12-04 01:26:13.145258','','XX1',1500000.00,'OCCUPIED','PRESIDENTIAL_SUITE','2025-12-08 13:00:40.621602',2);
/*!40000 ALTER TABLE `rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stays`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stays` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `checkin_at` datetime(6) NOT NULL,
  `checkout_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `reservation_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK74xu12pcnss76x46i3v9dtfhb` (`reservation_id`),
  KEY `FKlce89s1hymcut8ik43wty9ymu` (`room_id`),
  KEY `FK4liv21fcpv7heb88d38k7rcjm` (`user_id`),
  CONSTRAINT `FK4liv21fcpv7heb88d38k7rcjm` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK74xu12pcnss76x46i3v9dtfhb` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`),
  CONSTRAINT `FKlce89s1hymcut8ik43wty9ymu` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=73 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stays`
--

LOCK TABLES `stays` WRITE;
/*!40000 ALTER TABLE `stays` DISABLE KEYS */;
INSERT INTO `stays` VALUES (49,'2025-11-29 20:49:27.374510','2025-11-29 20:49:31.140137','2025-11-29 20:49:27.375516',92,42,24),(51,'2025-11-29 21:02:49.973499','2025-11-29 21:02:53.948121','2025-11-29 21:02:49.973917',94,41,24),(53,'2025-11-29 23:47:31.923191','2025-11-29 23:47:39.867167','2025-11-29 23:47:31.924185',97,42,24),(54,'2025-11-29 23:47:35.636208','2025-11-29 23:47:43.849431','2025-11-29 23:47:35.636208',98,42,24),(57,'2025-11-30 12:09:10.462511','2025-11-30 12:09:14.345541','2025-11-30 12:09:10.463374',103,41,24),(58,'2025-11-30 12:19:01.119146','2025-11-30 12:19:05.651073','2025-11-30 12:19:01.120140',104,42,24),(59,'2025-11-30 12:22:29.332392','2025-11-30 12:22:33.570876','2025-11-30 12:22:29.332392',105,41,24),(60,'2025-11-30 19:50:33.620518','2025-11-30 19:50:48.761479','2025-11-30 19:50:33.646886',106,42,24),(61,'2025-11-30 19:50:35.352962','2025-11-30 19:50:53.749193','2025-11-30 19:50:35.355021',107,42,24),(62,'2025-12-01 07:04:37.610165','2025-12-01 07:04:41.066911','2025-12-01 07:04:37.610898',108,41,24),(63,'2025-12-01 11:20:57.477306','2025-12-01 11:21:02.104523','2025-12-01 11:20:57.478845',109,42,25),(65,'2025-12-03 14:49:16.093592','2025-12-03 14:49:34.387962','2025-12-03 14:49:16.093592',122,43,24),(66,'2025-12-03 14:49:22.341664','2025-12-03 14:49:41.255017','2025-12-03 14:49:22.341664',123,42,24),(67,'2025-12-04 05:18:10.550033','2025-12-04 05:18:22.593228','2025-12-04 05:18:10.552597',129,52,35),(68,'2025-12-04 05:51:51.145129','2025-12-04 05:52:04.285173','2025-12-04 05:51:51.147153',132,45,36),(69,'2025-12-04 01:13:31.676351','2025-12-04 01:16:19.766999','2025-12-04 01:13:31.677063',134,42,40),(70,'2025-12-04 01:21:39.258720','2025-12-04 01:21:43.353137','2025-12-04 01:21:39.258866',135,47,24),(71,'2025-12-04 01:25:57.825262','2025-12-04 01:26:07.259138','2025-12-04 01:25:57.825443',136,52,41),(72,'2025-12-08 13:00:40.497310',NULL,'2025-12-08 13:00:40.497624',138,52,42);
/*!40000 ALTER TABLE `stays` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tamu`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tamu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgm4dqdd7gntxt9f8x4hs4594k` (`user_id`),
  CONSTRAINT `FK96fnijmuvlpka8wp67s6ipxu0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tamu`
--

LOCK TABLES `tamu` WRITE;
/*!40000 ALTER TABLE `tamu` DISABLE KEYS */;
INSERT INTO `tamu` VALUES (1,'2025-11-20 22:15:55.656078',5),(2,'2025-11-20 22:17:59.968197',17),(3,'2025-11-20 22:18:52.972780',2),(4,'2025-11-20 22:21:48.755481',19),(5,'2025-11-22 14:40:02.789639',15),(6,'2025-11-23 20:15:28.738993',1),(7,'2025-11-24 20:43:00.957959',20),(9,'2025-11-27 16:54:18.371627',22),(11,'2025-11-28 15:30:57.974874',24),(12,'2025-12-01 07:41:13.229081',25),(13,'2025-12-03 05:33:19.226530',26),(15,'2025-12-04 04:07:41.869013',34),(16,'2025-12-04 04:13:54.882651',35),(17,'2025-12-04 05:41:12.545000',36),(18,'2025-12-04 00:26:26.566128',38),(19,'2025-12-04 01:09:36.173124',40),(20,'2025-12-04 01:24:35.846451',41),(21,'2025-12-08 12:54:14.979537',42);
/*!40000 ALTER TABLE `tamu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tipe_kamar`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tipe_kamar` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(60) NOT NULL,
  `name` varchar(120) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmv4pghgx845nmxhwg0xvd2t75` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tipe_kamar`
--

LOCK TABLES `tipe_kamar` WRITE;
/*!40000 ALTER TABLE `tipe_kamar` DISABLE KEYS */;
INSERT INTO `tipe_kamar` VALUES (3,'DELUXE_KING','Deluxe King'),(4,'DELUXE_TWIN','Deluxe Twin'),(5,'SUITE_PANORAMA','Suite Panorama'),(6,'SUPERIOR_TWIN','Superior Twin'),(7,'STUDIO_LOFT','Studio Loft'),(8,'EXECUTIVE_SUITE','Executive Suite'),(9,'FAMILY_ROOM','Family Room'),(10,'PRESIDENTIAL_SUITE','Presidential Suite'),(11,'STANDARD','Standard'),(12,'VILLA','Villa');
/*!40000 ALTER TABLE `tipe_kamar` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(120) NOT NULL,
  `full_name` varchar(120) NOT NULL,
  `password_hash` varchar(60) NOT NULL,
  `role` enum('ADMIN','KARYAWAN','TAMU') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (2,'2025-10-30 21:12:32.727160','admin@gmail.com','Administrator','$2a$10$nfB83JmbRUqr4xxEQVyeYeANJW34WycSd7mx22gm.sE7TxAlUEwO2','ADMIN'),(17,'2025-11-08 00:37:47.375177','renza@gmail.com','renza','$2a$10$5HFDyT4DLCaQAMNY2V4MmOauDC.QIm34T8tLB7d6xIG06gpq7VBC.','KARYAWAN'),(19,'2025-11-20 22:21:15.830617','helena@gmail.com','helena','$2a$10$XTwuxcsQwOpsF9UrqjVXVeolX.2h45SGQ9mWmn7bsddv7BATtD96K','KARYAWAN'),(24,'2025-11-28 15:30:57.900791','nino@gmail.com','nino','$2a$10$BVPnQ58N23sLdOJcPIvdAux.jh9GzkuWZsWnDwmz.qkcsiRBijAui','TAMU'),(25,'2025-12-01 07:41:13.065787','123@gmail.com','Rayhan Nafish','$2a$10$PyOSMCM1mvCow9jQszWv4.Du1c5A8xVBglU8QFIWQTfWyVCXTg6.m','TAMU'),(26,'2025-12-03 05:33:19.190929','halo@gmail.com','Halo','$2a$10$9FiHHk5i.2RDrgIHottA0.Yc50rBptjnqBMpOu9MTUK/fdVQQqQl2','TAMU'),(33,'2025-12-04 03:41:21.459721','rendy@gmail.com','Rendy','$2a$10$baLshEQ4rm829/L1O4ksCunrHExD4yLZs425wGE0t2U2i7jFFzjD2','KARYAWAN'),(34,'2025-12-04 04:07:41.713785','sotit@gmail.com','Titos','$2a$10$xtF1Xc1wT22qblBWvcjTm.2kBbDcQPtezrIL.KMcjmV64Grpm2tgG','TAMU'),(35,'2025-12-04 04:13:54.747596','prabowo@gmail.com','prabowo','$2a$10$V.n7EgazBwXCCx3.fcOvAu1ooJ28qudtR658DWtCOWv/w9lLkDoAy','TAMU'),(36,'2025-12-04 05:41:12.355800','putin@gmail.com','putin','$2a$10$dXDjX2qyLoMa.LElYoNaJeZV65tb8oGT.v9X7jcGTuGQVrO3ejusG','TAMU'),(38,'2025-12-04 00:26:26.528242','helenakusumawardhani@gmail.com','Elena','$2a$10$DIzAeQ46uhb38HzFdaccg.WUCwfX1FlT.uJnqd.x6VNENVJrsKIwm','TAMU'),(40,'2025-12-04 01:09:36.137743','cuy@gmail.com','cuy','$2a$10$8Cm8rsM2eWJf4LomwYqSaewr1bfBBOMt3Jda0ppwwReSQPIKEfazC','TAMU'),(41,'2025-12-04 01:24:35.812786','trump@gmail.com','trump','$2a$10$e5Ha0MQCCUYIATXdTIG.4OTK9C1KB7/rf0BCEmuuKzvG.xDgz5sOq','TAMU'),(42,'2025-12-08 12:54:14.454656','ferdyowsem@gmail.com','cleo','$2a$10$FIoKEwFphw6MV98u9l5e8.Po7feO2Bg.fiRAcBJXyhHkh3EfXvFJG','TAMU');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'defaultdb'
--

--
-- Dumping routines for database 'defaultdb'
--
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-14 12:13:17
