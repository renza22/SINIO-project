-- MySQL dump 10.13  Distrib 9.4.0, for Win64 (x86_64)
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

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'ca91d90c-b559-11f0-ae38-862ccfb06803:1-92';

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
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
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2025-10-30 15:54:37.274488','rendy221205@gmail.com','Rendy Syahputra Riyadi','$2a$10$yZst96.ZdlHvmvCjTmqbruzU5WcmRJi7MiS1sKoUv6.WsdCDwvzKS','TAMU'),(2,'2025-10-30 21:12:32.727160','admin@gmail.com','Administrator','$2a$10$nfB83JmbRUqr4xxEQVyeYeANJW34WycSd7mx22gm.sE7TxAlUEwO2','ADMIN'),(3,'2025-10-30 21:26:00.617987','aan@gmail.com','AAN','$2a$10$BF4fhBwIEQnygFpj271eFOrwk1QagZWFo1gaviyaIo2/44d2nkPsi','TAMU'),(4,'2025-10-30 21:37:56.407354','bb@gmail.com','BB','$2a$10$Yc.B/6wp8mQMtGHoeMRuAe42ShyNy.rM570oY/O9b6Lu1lKRDrPzO','TAMU'),(5,'2025-10-30 23:47:43.121326','ok@gmail.com','eniac','$2a$10$03hA.3kefVzXmz3DEoceSel7FUAQKf5dyTk5.PH.ZvIZ.gQHA33Tq','TAMU'),(6,'2025-10-31 13:03:28.156840','k@gmail.com','Karyawan','$2a$10$xfjHTKy1yzc7Qx6Wh2gN9OBfHDs7XTkc.fB0cac0OBmaN2nclOnt6','KARYAWAN'),(8,'2025-10-31 15:31:29.717617','karyawan2@gmail.com','karyawan2','$2a$10$KFWe.LZJBcaXDic1x2tIaekekrriVUQiFDLohgz0.fOoY.0lC3Kb6','KARYAWAN'),(9,'2025-10-31 15:31:58.386571','karyawan3@gmail.com','karyawan3@gmail.com','$2a$10$Sw7n9WkEDkuaprX7LzX.ouvEnagUcnNUE/ob92CLW84vNS5NRK5fe','KARYAWAN'),(11,'2025-10-31 15:41:07.283017','renza@gmail.com','renza si karyawan','$2a$10$cShNE.BbyKvU.8oPkcgPHuDtQvsvZKqiJQ/w9WkUQd0tbS9H0iOue','KARYAWAN'),(12,'2025-10-31 08:42:59.000000','karyawan1@gmail.com','Karyawan 1','$','KARYAWAN'),(13,'2025-10-31 16:29:19.813658','karyawan99@gmail.com','karyawan top','$2a$10$bdVKo7VuhtUXetuPhGjWmOessQAQ/CDXCS2ul8h3BtonoVZOVtCn6','KARYAWAN');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'defaultdb'
--
--
-- WARNING: can't read the INFORMATION_SCHEMA.libraries table. It's most probably an old server 8.0.35.
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

-- Dump completed on 2025-10-31 20:15:54
