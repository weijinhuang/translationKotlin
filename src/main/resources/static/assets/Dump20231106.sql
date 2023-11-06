-- MySQL dump 10.13  Distrib 8.0.33, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: hwjtranslation
-- ------------------------------------------------------
-- Server version	8.0.33

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `client`
--

DROP TABLE IF EXISTS `client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client` (
  `ClientId` int NOT NULL,
  `NameTranId` int DEFAULT NULL,
  `AddressTranId` int DEFAULT NULL,
  `TelephoneNumber` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`ClientId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `client_multilanguages`
--

DROP TABLE IF EXISTS `client_multilanguages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client_multilanguages` (
  `CLId` int NOT NULL,
  `ClientId` int DEFAULT NULL,
  `Name` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `Address` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `Language` char(3) DEFAULT NULL,
  PRIMARY KEY (`CLId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_function_module`
--

DROP TABLE IF EXISTS `tb_function_module`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_function_module` (
  `moduleId` int NOT NULL AUTO_INCREMENT,
  `moduleName` varchar(15) DEFAULT NULL,
  `projectId` varchar(50) NOT NULL,
  PRIMARY KEY (`moduleId`,`projectId`),
  KEY `fk_TB_FUNCTION_MODULE_projectId` (`projectId`),
  CONSTRAINT `fk_TB_FUNCTION_MODULE_projectId` FOREIGN KEY (`projectId`) REFERENCES `tb_project` (`projectId`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_language`
--

DROP TABLE IF EXISTS `tb_language`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_language` (
  `languageId` int NOT NULL AUTO_INCREMENT,
  `languageName` varchar(50) DEFAULT NULL,
  `languageDes` varchar(20) DEFAULT NULL,
  `projectId` varchar(50) NOT NULL,
  PRIMARY KEY (`languageId`,`projectId`),
  KEY `fk_TB_LANGUAGE_projectId` (`projectId`),
  CONSTRAINT `fk_TB_LANGUAGE_projectId` FOREIGN KEY (`projectId`) REFERENCES `tb_project` (`projectId`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_project`
--

DROP TABLE IF EXISTS `tb_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_project` (
  `projectId` varchar(50) NOT NULL,
  `projectName` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`projectId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_translation`
--

DROP TABLE IF EXISTS `tb_translation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_translation` (
  `translationId` int NOT NULL AUTO_INCREMENT,
  `translationKey` varchar(100) DEFAULT NULL,
  `languageId` int DEFAULT NULL,
  `translationContent` varchar(500) DEFAULT NULL,
  `projectId` varchar(50) DEFAULT NULL,
  `moduleId` int DEFAULT NULL,
  PRIMARY KEY (`translationId`),
  KEY `fk_TB_FUNCTION_MODULE_moduleId` (`moduleId`),
  KEY `fk_TB_LANGUAGE_id` (`languageId`),
  KEY `fk_TB_PROJECT_projectId` (`projectId`),
  CONSTRAINT `fk_TB_FUNCTION_MODULE_moduleId` FOREIGN KEY (`moduleId`) REFERENCES `tb_function_module` (`moduleId`),
  CONSTRAINT `fk_TB_LANGUAGE_id` FOREIGN KEY (`languageId`) REFERENCES `tb_language` (`languageId`),
  CONSTRAINT `fk_TB_PROJECT_projectId` FOREIGN KEY (`projectId`) REFERENCES `tb_project` (`projectId`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2023-11-06 22:40:46
