-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 28, 2026 at 01:10 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `comicapp`
--

-- --------------------------------------------------------

--
-- Table structure for table `categories`
--

CREATE TABLE `categories` (
  `CategoryID` int(11) NOT NULL,
  `Name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `categories`
--

INSERT INTO `categories` (`CategoryID`, `Name`) VALUES
(1, 'Action'),
(2, 'Adventure'),
(4, 'Comedy'),
(3, 'Fantasy'),
(7, 'Historical'),
(5, 'Martial Arts'),
(9, 'Mystery'),
(8, 'Psychological'),
(11, 'Slice of Life'),
(6, 'Super Power'),
(10, 'Supernatural');

-- --------------------------------------------------------

--
-- Table structure for table `chapterimages`
--

CREATE TABLE `chapterimages` (
  `ImageID` int(11) NOT NULL,
  `ChapterID` int(11) NOT NULL,
  `ImageUrl` varchar(255) NOT NULL,
  `PageNumber` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `chapterimages`
--

INSERT INTO `chapterimages` (`ImageID`, `ChapterID`, `ImageUrl`, `PageNumber`) VALUES
(38, 3, 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEi8ysmvd_6CrhuWedu5SFIzIJhfXkhA7auXzQindM8PvSyLF-j79mkh-vuwe7lcSW6NH87dhVUToEhn_JYIYw_pttcao8yAz1tMxQA7hVOb0w62S_5wI5UFWm-QVEI91W2v_Bh_RRBRN8iC/s1600/NARUTO01_0003.jpg', 1),
(39, 3, 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj160VIisXkK4FCQVph_2NIYBTc-JVpj3AhDttl7fyOZwSI9JvnwCFpdexlJv3T2-haK37roPXM0Wqhx_SpEQMZilqOE06nGSXWXiQwts5dMGVhGyOVKpMJ6KweFbBHMk027YFCP-e5JiGb/s1600/NARUTO01_0004%252B0005.jpg', 2),
(40, 3, 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjSXPcx5lAicevln9AIan0OsIarCwnIl5VEtc_BlAsI2EE3ApKn3FW9wZqmC-bKIIVzNzm8A78dNDKcC2pAtl8Z6U898GEsGIxC7PC3SkCM4oC0jxDMT2n_DBLnfI9ro7oGlnDX1eicZFiD/s1600/NARUTO01_0008.jpg', 3),
(41, 3, 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgrudT9VZA0uOoAw4tyHJK8yUzhOxWCdmCitqfINGTDFC4Lv3D1B4VTNE-1gZ2wmPjLO7lFFPQNDjJ4qS6Uiseje5g1THB6fUgHFnr5ug9zm_uIkXUN260ABVpjt2hLQDTz9B4gUwDbyrde/s1600/NARUTO01_0009.jpg', 4),
(42, 3, 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEg2ayxEmFop6ne0rNXf0AN_q19MMjTeVUdX3U8kAp61NjF6PdoN12M1CONsoEmJfMxeYpL_vNAvkOYm7HTvj1DkW-EUV7iWrn9AnLFZ8sKcqdyTFipxKraYKqzLcnUBdMoTbTWUfcTHVTeG/s1600/NARUTO01_0010.jpg', 5),
(43, 3, 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgAnuqbYs9kVEofiFHCAU407MezayFd23Tp_DSt3rA_plK5jod-RWFM-HrsLhCuhkv5tzz5tsxKPsHQ7OWnDi03gOatQKlBJreqHWdz0_5KU0LYQfscaYScUXCvbeUEifBx3eoTP8gJ9LDC/s1600/NARUTO01_0011.jpg', 6),
(44, 3, 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjZTY_H_rY4W5C_kM87AK29KG89Q8fpHHoM5oBoirMrBnWYb7yAEXm8KoGTk5NgUth9TTayTOAYpCnhcV-X6YBEoUMCPATMvZ8Nju1IHE3RP1QTUr3385CWc41-sLZ37PwWHF1mxS2EeCef/s1600/NARUTO01_0012.jpg', 7),
(45, 3, 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjX6edh089OFEAqIjjxatBX5jE_77U2vHalKIWLIWbZbF3FyiB04gy0WzuVTZKIZv_qD59pu8yjDZx6ECAab6BFsC0ft4zo-c0OW92kkU2LQrnSW3fECwyjwrOo2HtNwLMlz6WBM1PY2mfh/s1600/NARUTO01_0013.jpg', 8),
(46, 3, 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhAxCfTVCBY6MvTrd8WlX5TP6vIH35P08ke2OXHsUd5nyjqPaMIhhlKrx9DSKDI0TpQfhep65a91KSy5MI22i5_hWV91rhjbGNw6wpaOs3wG40j3UtZia78fIqOOgnOY-cxRbXEpT2I68Ip/s1600/NARUTO01_0014.jpg', 9),
(47, 3, 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEisZJTCy-eACvkZukigPlGto3BU1pmSXpFbylQhn19z_3laZROgp4wER91q6F3y-ilW_xlQrt9lvyjeZPB06I_xp_pI8F_aNqs1hPBDDuTq4yEua7XlmMQGfuECQKMdNKBCpF-Q6FazEiOM/s1600/NARUTO01_0015.jpg', 10),
(48, 4, 'https://dilib.vn/img/comic/Naruto/img_00019.webp?v=4.90', 1),
(49, 4, 'https://dilib.vn/img/comic/Naruto/img_00020.webp?v=4.90', 2),
(50, 4, 'https://dilib.vn/img/comic/Naruto/img_00021.webp?v=4.90', 3),
(51, 4, 'https://dilib.vn/img/comic/Naruto/img_00022.webp?v=4.90', 4),
(52, 4, 'https://dilib.vn/img/comic/Naruto/img_00023.webp?v=4.90', 5),
(53, 4, 'https://dilib.vn/img/comic/Naruto/img_00024.webp?v=4.90', 6),
(54, 4, 'https://dilib.vn/img/comic/Naruto/img_00025.webp?v=4.90', 7),
(55, 4, 'https://dilib.vn/img/comic/Naruto/img_00026.webp?v=4.90', 8),
(56, 4, 'https://dilib.vn/img/comic/Naruto/img_00027.webp?v=4.90', 9),
(57, 4, 'https://dilib.vn/img/comic/Naruto/img_00028.webp?v=4.90', 10),
(69, 18, 'https://dilib.vn/img/comic/Tokyo.Ghoul/img_00000.webp?v=4.90', 1),
(70, 18, 'https://dilib.vn/img/comic/Tokyo.Ghoul/img_00001.webp?v=4.90', 2),
(71, 18, 'https://dilib.vn/img/comic/Tokyo.Ghoul/img_00002.webp?v=4.90', 3),
(72, 18, 'https://dilib.vn/img/comic/Tokyo.Ghoul/img_00003.webp?v=4.90', 4),
(73, 18, 'https://dilib.vn/img/comic/Tokyo.Ghoul/img_00004.webp?v=4.90', 5),
(74, 18, 'https://dilib.vn/img/comic/Tokyo.Ghoul/img_00005.webp?v=4.90', 6),
(75, 18, 'https://dilib.vn/img/comic/Tokyo.Ghoul/img_00006.webp?v=4.90', 7),
(76, 18, 'https://dilib.vn/img/comic/Tokyo.Ghoul/img_00007.webp?v=4.90', 8),
(77, 18, 'https://dilib.vn/img/comic/Tokyo.Ghoul/img_00008.webp?v=4.90', 9),
(79, 14, 'https://dilib.vn/img/comic/Doraemon.Dai.Tuyen.Tap/img_00002.webp?v=4.90', 1),
(80, 14, 'https://dilib.vn/img/comic/Doraemon.Dai.Tuyen.Tap/img_00003.webp?v=4.90', 2),
(81, 14, 'https://dilib.vn/img/comic/Doraemon.Dai.Tuyen.Tap/img_00004.webp?v=4.90', 3),
(82, 14, 'https://dilib.vn/img/comic/Doraemon.Dai.Tuyen.Tap/img_00005.webp?v=4.90', 4),
(83, 14, 'https://dilib.vn/img/comic/Doraemon.Dai.Tuyen.Tap/img_00006.webp?v=4.90', 5),
(84, 14, 'https://dilib.vn/img/comic/Doraemon.Dai.Tuyen.Tap/img_00007.webp?v=4.90', 6),
(85, 14, 'https://dilib.vn/img/comic/Doraemon.Dai.Tuyen.Tap/img_00008.webp?v=4.90', 7),
(86, 14, 'https://dilib.vn/img/comic/Doraemon.Dai.Tuyen.Tap/img_00009.webp?v=4.90', 8),
(87, 14, 'https://dilib.vn/img/comic/Doraemon.Dai.Tuyen.Tap/img_00010.webp?v=4.90', 9),
(88, 14, 'https://dilib.vn/img/comic/Doraemon.Dai.Tuyen.Tap/img_00011.webp?v=4.90', 10),
(89, 7, 'https://dilib.vn/img/comic/Dragon-Ball/img_00008.webp?v=4.90', 1),
(90, 7, 'https://dilib.vn/img/comic/Dragon-Ball/img_00009.webp?v=4.90', 2),
(91, 7, 'https://dilib.vn/img/comic/Dragon-Ball/img_00010.webp?v=4.90', 3),
(92, 7, 'https://dilib.vn/img/comic/Dragon-Ball/img_00011.webp?v=4.90', 4),
(93, 7, 'https://dilib.vn/img/comic/Dragon-Ball/img_00012.webp?v=4.90', 5),
(94, 7, 'https://dilib.vn/img/comic/Dragon-Ball/img_00013.webp?v=4.90', 6),
(95, 7, 'https://dilib.vn/img/comic/Dragon-Ball/img_00014.webp?v=4.90', 7),
(96, 7, 'https://dilib.vn/img/comic/Dragon-Ball/img_00015.webp?v=4.90', 8),
(97, 7, 'https://dilib.vn/img/comic/Dragon-Ball/img_00016.webp?v=4.90', 9),
(98, 7, 'https://dilib.vn/img/comic/Dragon-Ball/img_00017.webp?v=4.90', 10),
(99, 8, 'https://dilib.vn/img/comic/Dragon-Ball/img_00018.webp?v=4.90', 1),
(100, 8, 'https://dilib.vn/img/comic/Dragon-Ball/img_00019.webp?v=4.90', 2),
(101, 8, 'https://dilib.vn/img/comic/Dragon-Ball/img_00020.webp?v=4.90', 3),
(102, 8, 'https://dilib.vn/img/comic/Dragon-Ball/img_00021.webp?v=4.90', 4),
(103, 8, 'https://dilib.vn/img/comic/Dragon-Ball/img_00022.webp?v=4.90', 5),
(104, 8, 'https://dilib.vn/img/comic/Dragon-Ball/img_00023.webp?v=4.90', 6),
(105, 8, 'https://dilib.vn/img/comic/Dragon-Ball/img_00024.webp?v=4.90', 7),
(106, 8, 'https://dilib.vn/img/comic/Dragon-Ball/img_00025.webp?v=4.90', 8),
(107, 8, 'https://dilib.vn/img/comic/Dragon-Ball/img_00026.webp?v=4.90', 9),
(108, 8, 'https://dilib.vn/img/comic/Dragon-Ball/img_00027.webp?v=4.90', 10),
(109, 1, 'https://dilib.vn/img/comic/One-Piece/img_00002.webp?v=4.90', 1),
(110, 1, 'https://dilib.vn/img/comic/One-Piece/img_00003.webp?v=4.90', 2),
(111, 1, 'https://dilib.vn/img/comic/One-Piece/img_00004.webp?v=4.90', 3),
(112, 1, 'https://dilib.vn/img/comic/One-Piece/img_00005.webp?v=4.90', 4),
(113, 1, 'https://dilib.vn/img/comic/One-Piece/img_00006.webp?v=4.90', 5),
(114, 1, 'https://dilib.vn/img/comic/One-Piece/img_00007.webp?v=4.90', 6),
(115, 1, 'https://dilib.vn/img/comic/One-Piece/img_00008.webp?v=4.90', 7),
(116, 1, 'https://dilib.vn/img/comic/One-Piece/img_00009.webp?v=4.90', 8),
(117, 1, 'https://dilib.vn/img/comic/One-Piece/img_00010.webp?v=4.90', 9),
(118, 1, 'https://dilib.vn/img/comic/One-Piece/img_00011.webp?v=4.90', 10),
(119, 2, 'https://dilib.vn/img/comic/One-Piece/img_00012.webp?v=4.90', 1),
(120, 2, 'https://dilib.vn/img/comic/One-Piece/img_00013.webp?v=4.90', 2),
(121, 2, 'https://dilib.vn/img/comic/One-Piece/img_00014.webp?v=4.90', 3),
(122, 2, 'https://dilib.vn/img/comic/One-Piece/img_00015.webp?v=4.90', 4),
(123, 2, 'https://dilib.vn/img/comic/One-Piece/img_00016.webp?v=4.90', 5),
(124, 2, 'https://dilib.vn/img/comic/One-Piece/img_00017.webp?v=4.90', 6),
(125, 2, 'https://dilib.vn/img/comic/One-Piece/img_00018.webp?v=4.90', 7),
(126, 2, 'https://dilib.vn/img/comic/One-Piece/img_00019.webp?v=4.90', 8),
(127, 2, 'https://dilib.vn/img/comic/One-Piece/img_00020.webp?v=4.90', 9),
(128, 2, 'https://dilib.vn/img/comic/One-Piece/img_00021.webp?v=4.90', 10),
(129, 5, 'https://dilib.vn/img/comic/Bleach/img_00000.webp?v=4.90', 1),
(130, 5, 'https://dilib.vn/img/comic/Bleach/img_00001.webp?v=4.90', 2),
(131, 5, 'https://dilib.vn/img/comic/Bleach/img_00002.webp?v=4.90', 3),
(132, 5, 'https://dilib.vn/img/comic/Bleach/img_00003.webp?v=4.90', 4),
(133, 5, 'https://dilib.vn/img/comic/Bleach/img_00004.webp?v=4.90', 5),
(134, 5, 'https://dilib.vn/img/comic/Bleach/img_00005.webp?v=4.90', 6),
(135, 5, 'https://dilib.vn/img/comic/Bleach/img_00006.webp?v=4.90', 7),
(136, 5, 'https://dilib.vn/img/comic/Bleach/img_00007.webp?v=4.90', 8),
(137, 5, 'https://dilib.vn/img/comic/Bleach/img_00008.webp?v=4.90', 9),
(138, 5, 'https://dilib.vn/img/comic/Bleach/img_00009.webp?v=4.90', 10),
(139, 6, 'https://dilib.vn/img/comic/Bleach/img_00010.webp?v=4.90', 1),
(140, 6, 'https://dilib.vn/img/comic/Bleach/img_00011.webp?v=4.90', 2),
(141, 6, 'https://dilib.vn/img/comic/Bleach/img_00012.webp?v=4.90', 3),
(142, 6, 'https://dilib.vn/img/comic/Bleach/img_00013.webp?v=4.90', 4),
(143, 6, 'https://dilib.vn/img/comic/Bleach/img_00014.webp?v=4.90', 5),
(144, 6, 'https://dilib.vn/img/comic/Bleach/img_00015.webp?v=4.90', 6),
(145, 6, 'https://dilib.vn/img/comic/Bleach/img_00016.webp?v=4.90', 7),
(146, 6, 'https://dilib.vn/img/comic/Bleach/img_00017.webp?v=4.90', 8),
(147, 6, 'https://dilib.vn/img/comic/Bleach/img_00018.webp?v=4.90', 9),
(148, 6, 'https://dilib.vn/img/comic/Bleach/img_00019.webp?v=4.90', 10),
(149, 17, 'https://dilib.vn/img/comic/Shin-Cau-Be-But-Chi/img_00864.webp?v=4.90', 1),
(150, 17, 'https://dilib.vn/img/comic/Shin-Cau-Be-But-Chi/img_00865.webp?v=4.90', 2),
(151, 17, 'https://dilib.vn/img/comic/Shin-Cau-Be-But-Chi/img_00866.webp?v=4.90', 3),
(152, 17, 'https://dilib.vn/img/comic/Shin-Cau-Be-But-Chi/img_00867.webp?v=4.90', 4),
(153, 17, 'https://dilib.vn/img/comic/Shin-Cau-Be-But-Chi/img_00868.webp?v=4.90', 5),
(154, 17, 'https://dilib.vn/img/comic/Shin-Cau-Be-But-Chi/img_00869.webp?v=4.90', 6),
(155, 17, 'https://dilib.vn/img/comic/Shin-Cau-Be-But-Chi/img_00870.webp?v=4.90', 7),
(156, 17, 'https://dilib.vn/img/comic/Shin-Cau-Be-But-Chi/img_00871.webp?v=4.90', 8),
(157, 17, 'https://dilib.vn/img/comic/Shin-Cau-Be-But-Chi/img_00872.webp?v=4.90', 9),
(158, 17, 'https://dilib.vn/img/comic/Shin-Cau-Be-But-Chi/img_00873.webp?v=4.90', 10),
(159, 16, 'https://dilib.vn/img/comic/Monster/img_00000.webp?v=4.90', 1),
(160, 16, 'https://dilib.vn/img/comic/Monster/img_00001.webp?v=4.90', 2),
(161, 16, 'https://dilib.vn/img/comic/Monster/img_00002.webp?v=4.90', 3),
(162, 16, 'https://dilib.vn/img/comic/Monster/img_00003.webp?v=4.90', 4),
(163, 16, 'https://dilib.vn/img/comic/Monster/img_00004.webp?v=4.90', 5),
(164, 16, 'https://dilib.vn/img/comic/Monster/img_00005.webp?v=4.90', 6),
(165, 16, 'https://dilib.vn/img/comic/Monster/img_00006.webp?v=4.90', 7),
(166, 16, 'https://dilib.vn/img/comic/Monster/img_00007.webp?v=4.90', 8),
(167, 16, 'https://dilib.vn/img/comic/Monster/img_00008.webp?v=4.90', 9),
(168, 16, 'https://dilib.vn/img/comic/Monster/img_00009.webp?v=4.90', 10),
(169, 15, 'https://dilib.vn/img/comic/Vagabond/img_00000.webp?v=4.90', 1),
(170, 15, 'https://dilib.vn/img/comic/Vagabond/img_00001.webp?v=4.90', 2),
(171, 15, 'https://dilib.vn/img/comic/Vagabond/img_00002.webp?v=4.90', 3),
(172, 15, 'https://dilib.vn/img/comic/Vagabond/img_00003.webp?v=4.90', 4),
(173, 15, 'https://dilib.vn/img/comic/Vagabond/img_00004.webp?v=4.90', 5),
(174, 15, 'https://dilib.vn/img/comic/Vagabond/img_00005.webp?v=4.90', 6),
(175, 15, 'https://dilib.vn/img/comic/Vagabond/img_00006.webp?v=4.90', 7),
(176, 15, 'https://dilib.vn/img/comic/Vagabond/img_00007.webp?v=4.90', 8),
(177, 15, 'https://dilib.vn/img/comic/Vagabond/img_00008.webp?v=4.90', 9),
(178, 15, 'https://dilib.vn/img/comic/Vagabond/img_00009.webp?v=4.90', 10);

-- --------------------------------------------------------

--
-- Table structure for table `chapters`
--

CREATE TABLE `chapters` (
  `ChapterID` int(11) NOT NULL,
  `ComicID` int(11) NOT NULL,
  `ChapterNumber` float NOT NULL,
  `Title` varchar(255) DEFAULT NULL,
  `ViewCount` int(11) DEFAULT 0,
  `CreatedAt` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `chapters`
--

INSERT INTO `chapters` (`ChapterID`, `ComicID`, `ChapterNumber`, `Title`, `ViewCount`, `CreatedAt`) VALUES
(1, 1, 1, 'Romance Dawn', 120003, '2026-06-12 01:28:42'),
(2, 1, 2, 'Họ xuất hiện', 110004, '2026-06-12 01:28:42'),
(3, 2, 1, 'Uzumaki Naruto', 100010, '2026-06-12 01:28:42'),
(4, 2, 2, 'Konohamaru', 95008, '2026-06-12 01:28:42'),
(5, 3, 1, 'Death and Strawberry', 85001, '2026-06-12 01:28:42'),
(6, 3, 2, 'Starter', 82001, '2026-06-12 01:28:42'),
(7, 4, 1, 'Bulma và Son Goku', 150001, '2026-06-12 01:28:42'),
(8, 4, 2, 'Cuộc hành trình bắt đầu', 145001, '2026-06-12 01:28:42'),
(14, 8, 1, 'Chapter 1', 120500, '2026-06-28 01:12:30'),
(15, 9, 1, 'Chapter 1', 894000, '2026-06-28 01:12:30'),
(16, 10, 1, 'Chapter 1', 750000, '2026-06-28 01:12:30'),
(17, 11, 47, '', 340000, '2026-06-28 01:12:30'),
(18, 12, 1, 'Chapter 1', 1850000, '2026-06-28 01:12:30'),
(19, 13, 1, 'Chapter 1', 230000, '2026-06-28 01:12:30'),
(20, 14, 1, 'Chapter 1', 410000, '2026-06-28 01:12:30'),
(21, 15, 1, 'Chapter 1', 920000, '2026-06-28 01:12:30');

-- --------------------------------------------------------

--
-- Table structure for table `comics`
--

CREATE TABLE `comics` (
  `ComicID` int(11) NOT NULL,
  `Title` varchar(255) NOT NULL,
  `Author` varchar(255) DEFAULT NULL,
  `Description` text DEFAULT NULL,
  `CoverImageUrl` varchar(255) DEFAULT NULL,
  `ViewCount` int(11) DEFAULT 0,
  `Rating` float DEFAULT 0,
  `Status` varchar(255) DEFAULT NULL,
  `IsHidden` tinyint(1) DEFAULT 0,
  `CreatedAt` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `comics`
--

INSERT INTO `comics` (`ComicID`, `Title`, `Author`, `Description`, `CoverImageUrl`, `ViewCount`, `Rating`, `Status`, `IsHidden`, `CreatedAt`) VALUES
(1, 'One Piece', 'Eiichiro Oda', 'Hành trình trở thành Vua Hải Tặc của Monkey D. Luffy.', 'https://cdn.myanimelist.net/images/manga/2/253146.jpg', 5000008, 4.9, 'Ongoing', 0, '2026-06-12 01:28:41'),
(2, 'Naruto', 'Masashi Kishimoto', 'Câu chuyện về ninja Naruto Uzumaki.', 'http://localhost:8080/uploads/covers/cover_1781376147240.jpg', 4200021, 4.3, 'Completed', 0, '2026-06-12 01:28:41'),
(3, 'Bleach', 'Tite Kubo', 'Hành trình của Shinigami Ichigo Kurosaki.', 'https://dilib.vn/img/news/2024/03/larger/14753-bleach-su-mang-than-chet-1.webp?v=4849', 3100002, 4.7, 'Completed', 0, '2026-06-12 01:28:41'),
(4, 'Dragon Ball', 'Akira Toriyama', 'Cuộc phiêu lưu tìm ngọc rồng của Son Goku.', 'http://localhost:8080/uploads/covers/cover_1782614244862.jpg', 7000002, 4.9, 'Completed', 0, '2026-06-12 01:28:41'),
(8, 'Doraemon', 'Fujiko F. Fujio', 'Chú mèo máy Doraemon từ thế kỷ 22 giúp Nobita bằng những bảo bối thần kỳ.', 'https://upload.wikimedia.org/wikipedia/en/c/c8/Doraemon_volume_1_cover.jpg', 120500, 4.8, 'Completed', 0, '2026-06-27 19:33:20'),
(9, 'Vagabond', 'Takehiko Inoue', 'Hành trình trở thành kiếm sĩ vĩ đại của Miyamoto Musashi.', 'https://dilib.vn/img/news/2026/01/larger/5566-lang-khach-vagabond-1.webp?v=4314', 894000, 4.9, 'Hiatus', 0, '2026-06-27 19:33:20'),
(10, 'Monster', 'Naoki Urasawa', 'Một bác sĩ vô tình cứu sống kẻ sát nhân hàng loạt và bị cuốn vào chuỗi bi kịch.', 'https://dilib.vn/img/news/2024/03/larger/14756-quai-vat-monster-1.webp?v=7322', 750000, 4.8, 'Completed', 0, '2026-06-27 19:33:20'),
(11, 'Crayon Shin-chan', 'Yoshito Usui', 'Những câu chuyện hài hước về cậu bé Shin và gia đình.', 'https://dilib.vn/img/news/2024/03/larger/14787-shin-cau-be-but-chi-1.webp?v=4544', 340000, 4.5, 'Completed', 0, '2026-06-27 19:33:20'),
(12, 'Tokyo Ghoul', 'Sui Ishida', 'Ken Kaneki trở thành nửa người nửa Ghoul sau một tai nạn.', 'https://dilib.vn/img/news/2026/01/larger/5572-nga-quy-tokyo-tokyo-ghoul-1.webp?v=3238', 1850000, 4.6, 'Completed', 0, '2026-06-27 19:33:20'),
(13, 'Oyasumi Punpun', 'Inio Asano', 'Câu chuyện trưởng thành đầy u tối của Punpun.', 'http://localhost:8080/uploads/covers/cover_1782616144103.jpg', 230000, 4.7, 'Completed', 0, '2026-06-27 19:33:20'),
(14, 'Bakemonogatari', 'Nisio Isin', 'Loạt truyện siêu nhiên xoay quanh Araragi Koyomi và những hiện tượng kỳ bí.', 'http://localhost:8080/uploads/covers/cover_1782615921538.jpg', 410000, 4.4, 'Completed', 0, '2026-06-27 19:33:20'),
(15, 'JoJo\'s Bizarre Adventure', 'Hirohiko Araki', 'Cuộc phiêu lưu kỳ lạ của gia tộc Joestar qua nhiều thế hệ.', 'https://dilib.vn/img/news/2024/06/larger/16068-cuoc-phieu-luu-ky-bi-cua-jojo-1.webp?v=2301', 920000, 4.8, 'Ongoing', 0, '2026-06-27 19:33:20');

-- --------------------------------------------------------

--
-- Table structure for table `comic_categories`
--

CREATE TABLE `comic_categories` (
  `ComicID` int(11) NOT NULL,
  `CategoryID` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `comic_categories`
--

INSERT INTO `comic_categories` (`ComicID`, `CategoryID`) VALUES
(1, 1),
(1, 2),
(1, 3),
(3, 1),
(3, 3),
(3, 6),
(8, 4),
(8, 11),
(9, 1),
(9, 5),
(9, 7),
(10, 8),
(10, 9),
(11, 4),
(11, 11),
(12, 1),
(12, 8),
(12, 10),
(13, 8),
(13, 11),
(14, 9),
(14, 10),
(15, 1),
(15, 2),
(15, 6);

-- --------------------------------------------------------

--
-- Table structure for table `comments`
--

CREATE TABLE `comments` (
  `CommentID` int(11) NOT NULL,
  `UserID` int(11) NOT NULL,
  `ComicID` int(11) DEFAULT NULL,
  `ChapterID` int(11) DEFAULT NULL,
  `ParentCommentID` int(11) DEFAULT NULL,
  `Content` text NOT NULL,
  `ReplyCount` int(11) DEFAULT 0,
  `LikeCount` int(11) DEFAULT 0,
  `DislikeCount` int(11) DEFAULT 0,
  `ReportCount` int(11) DEFAULT 0,
  `IsDeleted` tinyint(1) DEFAULT 0,
  `CreatedAt` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `comments`
--

INSERT INTO `comments` (`CommentID`, `UserID`, `ComicID`, `ChapterID`, `ParentCommentID`, `Content`, `ReplyCount`, `LikeCount`, `DislikeCount`, `ReportCount`, `IsDeleted`, `CreatedAt`) VALUES
(1, 2, 1, 1, NULL, 'One Piece quá hay!', 0, 12, 0, 0, 0, '2026-06-12 01:28:42'),
(2, 3, 2, 3, NULL, 'Naruto là tuổi thơ của tôi.', 1, 8, 0, 1, 0, '2026-06-12 01:28:42'),
(3, 4, 4, 7, NULL, 'Dragon Ball huyền thoại.', 0, 15, 0, 0, 0, '2026-06-12 01:28:42'),
(4, 3, 1, 1, 1, 'Đồng ý luôn!', 0, 0, 0, 0, 0, '2026-06-12 01:28:42'),
(5, 6, 2, NULL, 2, '@Minh lọ', 0, 0, 0, 0, 0, '2026-06-14 01:59:41'),
(6, 6, 2, NULL, NULL, 'thua goku', 5, 1, 0, 0, 0, '2026-06-19 18:40:16'),
(7, 5, 2, NULL, NULL, '@HuyBui nigga', 2, 1, 0, 0, 0, '2026-06-20 00:27:42'),
(9, 6, 2, NULL, 6, '@HuyBui test', 0, 0, 0, 0, 0, '2026-06-20 00:28:29'),
(12, 6, 2, NULL, 6, '@HuyBui p2', 0, 0, 0, 0, 1, '2026-06-20 10:12:40'),
(15, 6, 2, NULL, NULL, 'test', 0, 0, 0, 0, 1, '2026-06-25 23:39:21'),
(18, 6, 2, NULL, 7, '@YuhBui racist', 0, 0, 0, 0, 0, '2026-06-26 00:51:16'),
(20, 6, 2, 4, NULL, 'first', 1, 0, 0, 0, 0, '2026-06-26 00:57:49'),
(25, 6, 2, NULL, 6, '@HuyBui janc', 0, 0, 0, 0, 1, '2026-06-26 10:16:25'),
(27, 5, 2, NULL, 20, '@HuyBui', 0, 0, 0, 0, 1, '2026-06-26 10:39:26'),
(28, 6, 2, NULL, 6, '@HuyBui test 2', 0, 0, 0, 0, 0, '2026-06-26 10:51:10'),
(29, 6, 1, NULL, NULL, 'ncc', 1, 0, 0, 0, 0, '2026-06-26 10:51:29'),
(30, 5, 1, NULL, 29, '@HuyBui ncc', 0, 0, 0, 1, 0, '2026-06-26 10:51:51'),
(31, 5, 2, NULL, NULL, '@HuyBui: bcd', 0, 0, 0, 0, 0, '2026-06-26 11:25:24'),
(33, 5, 2, NULL, NULL, '@HuyBui xyz', 0, 0, 0, 0, 0, '2026-06-26 16:10:20'),
(39, 6, 2, 3, NULL, 'qwerty', 5, 0, 0, 0, 0, '2026-06-26 16:34:46'),
(43, 5, NULL, 3, 39, '@HuyBui one', 0, 0, 0, 0, 0, '2026-06-27 00:44:07'),
(44, 5, NULL, 3, 39, '@HuyBui two', 0, 0, 0, 0, 0, '2026-06-27 00:57:55'),
(45, 6, 2, NULL, 6, '@HuyBui test 3', 0, 0, 0, 0, 1, '2026-06-27 01:13:19'),
(46, 2, 8, 14, NULL, 'Doraemon đọc đi đọc lại từ bé đến lớn vẫn thấy lôi cuốn, đúng là huyền thoại!', 0, 45, 0, 0, 0, '2026-06-28 16:16:19'),
(47, 3, 8, 14, NULL, 'Ước gì mình cũng có một chiếc túi thần kỳ giống Nobita nhỉ haha.', 0, 23, 1, 0, 0, '2026-06-28 16:16:19'),
(48, 4, 9, 15, NULL, 'Nét vẽ và chiều sâu nội tâm nhân vật của Takehiko Inoue thực sự là một kiệt tác nghệ thuật đỉnh cao.', 0, 89, 0, 0, 0, '2026-06-28 16:16:19'),
(49, 6, 10, 16, NULL, 'Monster là bộ manga trinh thám tâm lý xuất sắc nhất tôi từng xem, hình tượng Johan bộc lộ chiều sâu quá kinh điển.', 0, 112, 2, 0, 0, '2026-06-28 16:16:19'),
(50, 2, 11, 17, NULL, 'Cu cu cu cu cu cu Shin làm mình cười đau cả bụng, độ bựa không ai bằng luôn á!', 0, 34, 0, 0, 0, '2026-06-28 16:16:19'),
(51, 3, 12, 18, NULL, 'Kaneki bất hạnh quá, đọc đến đoạn này bài Unravel tự động vang lên trong đầu luôn.', 0, 76, 4, 0, 0, '2026-06-28 16:16:19'),
(52, 4, 13, 19, NULL, 'Một bộ truyện cực kỳ thực tế và sâu sắc, nhưng cảnh báo ai tâm lý yếu đọc xong dễ trầm cảm mất mấy ngày đấy.', 0, 54, 1, 0, 0, '2026-06-28 16:16:19'),
(53, 6, 15, 21, NULL, 'Is that a JoJo reference?! Thần thái và dáng pose không lẫn đi đâu được, chất quá!', 0, 98, 0, 0, 0, '2026-06-28 16:16:19'),
(54, 3, 8, 14, NULL, 'Nostalgia quá, nhớ hồi nhỏ cứ nhịn ăn sáng để gom tiền mua từng cuốn mỏng bọc bìa kiếng.', 2, 56, 1, 0, 0, '2026-06-28 14:41:12'),
(55, 4, 8, 14, NULL, 'Mấy tập ngắn này đọc giải trí nhẹ nhàng trước khi ngủ là hết bài.', 0, 12, 0, 0, 0, '2026-06-28 15:41:12'),
(56, 2, 9, 15, NULL, 'Trận đấu ở chap này nghẹt thở thực sự, nhịp truyện chậm nhưng combat chất lừ.', 1, 94, 0, 0, 0, '2026-06-28 11:41:12'),
(57, 6, 9, 15, NULL, 'Tác giả vẽ bằng cọ lông hay sao ấy nhỉ, nhìn từng nét mực đổ bóng đỉnh dã man.', 0, 42, 2, 0, 0, '2026-06-28 13:41:12'),
(58, 4, 10, 16, NULL, 'Johan Liebert đúng là phản diện quyến rũ nhất mọi thời đại, lạnh lùng mà cuốn hút.', 1, 130, 1, 0, 0, '2026-06-28 12:41:12'),
(59, 2, 10, 16, NULL, 'Bác sĩ Tenma quả là người có đức tin tuyệt đối vào giá trị mạng sống, xem mà nể phục tính cách của bác.', 0, 67, 0, 0, 0, '2026-06-28 14:41:12'),
(60, 6, 11, 17, NULL, 'Shin lém lỉnh kinh khủng, cơ mà nhiều lúc ông cu cậu cũng ấm áp biết quan tâm bố mẹ lắm.', 1, 39, 0, 0, 0, '2026-06-28 10:41:12'),
(61, 2, 12, 18, NULL, 'Đọc bản manga chi tiết và bạo lực hơn anime nhiều, art của Sui Ishida càng về sau càng như tranh trừu tượng.', 1, 85, 3, 0, 0, '2026-06-28 13:41:12'),
(62, 3, 13, 19, NULL, 'Mới đọc vài chap đầu thấy vui vui mà càng về sau không khí truyện càng ngột ngạt, ám ảnh thật sự.', 1, 72, 0, 0, 0, '2026-06-28 08:41:12'),
(63, 2, 14, 20, NULL, 'Senjougahara Hitagi đúng chuẩn mẫu waifu tsundere thời kỳ đầu, thoại của bộ này nghe bánh cuốn ghê.', 0, 29, 1, 0, 0, '2026-06-28 15:41:12'),
(64, 4, 14, 20, NULL, 'Art của Oh! great vẽ thì đẹp xuất sắc miễn bàn rồi, thiết kế nhân vật nhìn mướt mắt dã man.', 0, 41, 0, 0, 0, '2026-06-28 16:11:12'),
(65, 3, 15, 21, NULL, 'Càng xem càng bị cuốn vào cái tư duy chiến đấu bằng Stand của tác giả, hack não cực kỳ!', 1, 105, 1, 0, 0, '2026-06-28 09:41:12'),
(66, 2, 8, 14, 47, '@Minh Chuẩn luôn b ơi, hồi đó còn có vụ đổi truyện cũ lấy truyện mới ngoài tiệm sách nữa.', 0, 15, 0, 0, 0, '2026-06-28 16:41:12'),
(67, 6, 8, 14, 47, '@Minh Đọc bộ này xong hồi nhỏ cứ hay cạy ngăn bàn ra xem có cỗ máy thời gian không haha.', 0, 22, 0, 0, 0, '2026-06-28 16:41:12'),
(68, 4, 9, 15, 56, '@Huy Chuẩn b, Musashi bắt đầu ngộ ra được triết lý kiếm đạo rồi, không còn chỉ biết lao vào chém giết nữa.', 0, 31, 0, 0, 0, '2026-06-28 16:41:12'),
(69, 3, 10, 16, 58, '@An Đúng vậy, sự đáng sợ của Johan nằm ở chỗ hắn thao túng tâm lý người khác quá nhẹ nhàng.', 0, 48, 0, 0, 0, '2026-06-28 16:41:12'),
(70, 2, 11, 17, 60, '@HuyBui Nhớ tập Shin đi chợ giúp mẹ hay tập bảo vệ em Himawari xem cảm động thực sự.', 0, 19, 0, 0, 0, '2026-06-28 16:41:12'),
(71, 6, 12, 18, 61, '@Huy Tiếc là phần anime cắt xén nhiều tình tiết quá làm mất đi độ logic vốn có của mạch truyện.', 0, 25, 0, 0, 0, '2026-06-28 16:41:12'),
(72, 4, 13, 19, 62, '@Minh Công nhận luôn, tác giả lồng ghép các yếu tố khủng hoảng hiện sinh hiện thực quá mức.', 0, 18, 0, 0, 0, '2026-06-28 16:41:13'),
(73, 2, 15, 21, 65, '@Minh Sang mấy phần sau năng lực Stand tiến hóa ảo ma lắm b ơi, đọc không rời mắt được đâu.', 0, 37, 0, 0, 0, '2026-06-28 16:41:13'),
(74, 3, 8, 14, NULL, 'Nostalgia quá, nhớ hồi nhỏ cứ nhịn ăn sáng để gom tiền mua từng cuốn mỏng bọc bìa kiếng.', 2, 56, 1, 0, 0, '2026-06-28 14:41:13'),
(75, 4, 8, 14, NULL, 'Mấy tập ngắn này đọc giải trí nhẹ nhàng trước khi ngủ là hết bài.', 0, 12, 0, 0, 0, '2026-06-28 15:41:13'),
(76, 2, 9, 15, NULL, 'Trận đấu ở chap này nghẹt thở thực sự, nhịp truyện chậm nhưng combat chất lừ.', 1, 94, 0, 0, 0, '2026-06-28 11:41:13'),
(77, 6, 9, 15, NULL, 'Tác giả vẽ bằng cọ lông hay sao ấy nhỉ, nhìn từng nét mực đổ bóng đỉnh dã man.', 0, 42, 2, 0, 0, '2026-06-28 13:41:13'),
(78, 4, 10, 16, NULL, 'Johan Liebert đúng là phản diện quyến rũ nhất mọi thời đại, lạnh lùng mà cuốn hút.', 1, 130, 1, 0, 0, '2026-06-28 12:41:13'),
(79, 2, 10, 16, NULL, 'Bác sĩ Tenma quả là người có đức tin tuyệt đối vào giá trị mạng sống, xem mà nể phục tính cách của bác.', 0, 67, 0, 0, 0, '2026-06-28 14:41:13'),
(80, 6, 11, 17, NULL, 'Shin lém lỉnh kinh khủng, cơ mà nhiều lúc ông cu cậu cũng ấm áp biết quan tâm bố mẹ lắm.', 1, 39, 0, 0, 0, '2026-06-28 10:41:13'),
(81, 2, 12, 18, NULL, 'Đọc bản manga chi tiết và bạo lực hơn anime nhiều, art của Sui Ishida càng về sau càng như tranh trừu tượng.', 1, 85, 3, 0, 0, '2026-06-28 13:41:13'),
(82, 3, 13, 19, NULL, 'Mới đọc vài chap đầu thấy vui vui mà càng về sau không khí truyện càng ngột ngạt, ám ảnh thật sự.', 1, 72, 0, 0, 0, '2026-06-28 08:41:13'),
(83, 2, 14, 20, NULL, 'Senjougahara Hitagi đúng chuẩn mẫu waifu tsundere thời kỳ đầu, thoại của bộ này nghe bánh cuốn ghê.', 0, 29, 1, 0, 0, '2026-06-28 15:41:13'),
(84, 4, 14, 20, NULL, 'Art của Oh! great vẽ thì đẹp xuất sắc miễn bàn rồi, thiết kế nhân vật nhìn mướt mắt dã man.', 0, 41, 0, 0, 0, '2026-06-28 16:11:13'),
(85, 3, 15, 21, NULL, 'Càng xem càng bị cuốn vào cái tư duy chiến đấu bằng Stand của tác giả, hack não cực kỳ!', 1, 105, 1, 0, 0, '2026-06-28 09:41:13'),
(86, 2, 8, 14, 47, '@Minh Chuẩn luôn b ơi, hồi đó còn có vụ đổi truyện cũ lấy truyện mới ngoài tiệm sách nữa.', 0, 15, 0, 0, 0, '2026-06-28 16:41:13'),
(87, 6, 8, 14, 47, '@Minh Đọc bộ này xong hồi nhỏ cứ hay cạy ngăn bàn ra xem có cỗ máy thời gian không haha.', 0, 22, 0, 0, 0, '2026-06-28 16:41:13'),
(88, 4, 9, 15, 56, '@Huy Chuẩn b, Musashi bắt đầu ngộ ra được triết lý kiếm đạo rồi, không còn chỉ biết lao vào chém giết nữa.', 0, 31, 0, 0, 0, '2026-06-28 16:41:13'),
(89, 3, 10, 16, 58, '@An Đúng vậy, sự đáng sợ của Johan nằm ở chỗ hắn thao túng tâm lý người khác quá nhẹ nhàng.', 0, 48, 0, 0, 0, '2026-06-28 16:41:13'),
(90, 2, 11, 17, 60, '@HuyBui Nhớ tập Shin đi chợ giúp mẹ hay tập bảo vệ em Himawari xem cảm động thực sự.', 0, 19, 0, 0, 0, '2026-06-28 16:41:13'),
(91, 6, 12, 18, 61, '@Huy Tiếc là phần anime cắt xén nhiều tình tiết quá làm mất đi độ logic vốn có của mạch truyện.', 0, 25, 0, 0, 0, '2026-06-28 16:41:13'),
(92, 4, 13, 19, 62, '@Minh Công nhận luôn, tác giả lồng ghép các yếu tố khủng hoảng hiện sinh hiện thực quá mức.', 0, 18, 0, 0, 0, '2026-06-28 16:41:13'),
(93, 2, 15, 21, 65, '@Minh Sang mấy phần sau năng lực Stand tiến hóa ảo ma lắm b ơi, đọc không rời mắt được đâu.', 0, 37, 0, 0, 0, '2026-06-28 16:41:13');

-- --------------------------------------------------------

--
-- Table structure for table `comment_interactions`
--

CREATE TABLE `comment_interactions` (
  `InteractionID` int(11) NOT NULL,
  `UserID` int(11) NOT NULL,
  `CommentID` int(11) NOT NULL,
  `InteractionType` int(11) NOT NULL,
  `CreatedAt` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `comment_interactions`
--

INSERT INTO `comment_interactions` (`InteractionID`, `UserID`, `CommentID`, `InteractionType`, `CreatedAt`) VALUES
(1, 3, 1, 1, '2026-06-12 01:28:42'),
(2, 4, 1, 1, '2026-06-12 01:28:42'),
(3, 2, 3, 1, '2026-06-12 01:28:42'),
(4, 4, 2, 1, '2026-06-12 01:28:42'),
(28, 5, 6, 1, '2026-06-20 01:40:00'),
(29, 6, 7, 1, '2026-06-20 09:56:39');

-- --------------------------------------------------------

--
-- Table structure for table `comment_reports`
--

CREATE TABLE `comment_reports` (
  `ReportID` int(11) NOT NULL,
  `UserID` int(11) NOT NULL,
  `CommentID` int(11) NOT NULL,
  `Reason` varchar(255) NOT NULL,
  `IsResolved` tinyint(1) DEFAULT 0,
  `CreatedAt` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `comment_reports`
--

INSERT INTO `comment_reports` (`ReportID`, `UserID`, `CommentID`, `Reason`, `IsResolved`, `CreatedAt`) VALUES
(1, 2, 2, 'Spam', 0, '2026-06-12 01:28:42'),
(2, 6, 2, 'test', 0, '2026-06-26 00:53:47'),
(3, 6, 30, 'sv', 0, '2026-06-26 10:52:30');

-- --------------------------------------------------------

--
-- Table structure for table `follows`
--

CREATE TABLE `follows` (
  `UserID` int(11) NOT NULL,
  `ComicID` int(11) NOT NULL,
  `IsNotificationOn` tinyint(1) DEFAULT 1,
  `FollowedAt` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `follows`
--

INSERT INTO `follows` (`UserID`, `ComicID`, `IsNotificationOn`, `FollowedAt`) VALUES
(2, 1, 1, '2026-06-12 01:28:42'),
(2, 2, 1, '2026-06-12 01:28:42'),
(3, 1, 1, '2026-06-12 01:28:42'),
(3, 4, 1, '2026-06-12 01:28:42'),
(4, 3, 1, '2026-06-12 01:28:42'),
(6, 2, 1, '2026-06-21 01:37:16');

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `NotificationID` int(11) NOT NULL,
  `UserID` int(11) NOT NULL,
  `ComicID` int(11) DEFAULT NULL,
  `Title` varchar(255) NOT NULL,
  `Message` varchar(255) NOT NULL,
  `IsRead` tinyint(1) DEFAULT 0,
  `CreatedAt` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `notifications`
--

INSERT INTO `notifications` (`NotificationID`, `UserID`, `ComicID`, `Title`, `Message`, `IsRead`, `CreatedAt`) VALUES
(1, 2, 1, 'One Piece có chương mới', 'Chương mới vừa được cập nhật', 0, '2026-06-12 01:28:42'),
(2, 3, 4, 'Dragon Ball nổi bật', 'Truyện đang thịnh hành tuần này', 0, '2026-06-12 01:28:42'),
(3, 2, 2, 'Cập nhật thông tin truyện', 'Truyện \'Naruto\' bạn yêu thích vừa được cập nhật thông tin mới.', 0, '2026-06-14 01:42:27'),
(4, 1, NULL, 'Test', 'abc', 0, '2026-06-20 16:05:40'),
(5, 2, NULL, 'Test', 'abc', 0, '2026-06-20 16:05:40'),
(6, 3, NULL, 'Test', 'abc', 0, '2026-06-20 16:05:40'),
(7, 4, NULL, 'Test', 'abc', 0, '2026-06-20 16:05:40'),
(8, 5, NULL, 'Test', 'abc', 0, '2026-06-20 16:05:40'),
(9, 6, NULL, 'Test', 'abc', 1, '2026-06-20 16:05:40'),
(10, 2, 2, 'Truyện theo dõi có chương mới!', 'Bộ truyện \'Naruto\' bạn thích vừa ra mắt Chương 3.0. Đọc ngay thôi!', 0, '2026-06-21 23:57:11'),
(11, 6, 2, 'Truyện theo dõi có chương mới!', 'Bộ truyện \'Naruto\' bạn thích vừa ra mắt Chương 3.0. Đọc ngay thôi!', 1, '2026-06-21 23:57:11'),
(12, 2, 1, 'abc', 'xyz', 0, '2026-06-22 18:07:13'),
(13, 3, 1, 'abc', 'xyz', 0, '2026-06-22 18:07:13'),
(14, 2, 2, 'test 1', 'siuuu', 0, '2026-06-22 18:07:33'),
(15, 6, 2, 'test 1', 'siuuu', 1, '2026-06-22 18:07:33'),
(16, 1, NULL, 'all', 'jdnfb', 0, '2026-06-22 18:07:40'),
(17, 2, NULL, 'all', 'jdnfb', 0, '2026-06-22 18:07:40'),
(18, 3, NULL, 'all', 'jdnfb', 0, '2026-06-22 18:07:40'),
(19, 4, NULL, 'all', 'jdnfb', 0, '2026-06-22 18:07:40'),
(20, 5, NULL, 'all', 'jdnfb', 0, '2026-06-22 18:07:40'),
(21, 6, NULL, 'all', 'jdnfb', 1, '2026-06-22 18:07:40'),
(22, 2, 2, 'zxc', 'vbnm', 0, '2026-06-23 16:23:13'),
(23, 6, 2, 'zxc', 'vbnm', 1, '2026-06-23 16:23:13'),
(24, 3, 4, 'Truyện theo dõi có chương mới!', 'Bộ truyện \'Dragon Ball\' bạn thích vừa ra mắt Chương 2.5: jsbfb. Đọc ngay thôi!', 0, '2026-06-23 23:54:22'),
(25, 2, 2, 'Truyện theo dõi có chương mới!', 'Bộ truyện \'Naruto\' bạn thích vừa ra mắt Chương 4.0: abc. Khám phá ngay!', 0, '2026-06-25 17:25:26'),
(26, 6, 2, 'Truyện theo dõi có chương mới!', 'Bộ truyện \'Naruto\' bạn thích vừa ra mắt Chương 4.0: abc. Khám phá ngay!', 1, '2026-06-25 17:25:26'),
(27, 2, 1, 'Truyện theo dõi có chương mới!', 'Bộ truyện \'One Piece\' bạn thích vừa ra mắt Chương 3.0. Khám phá ngay!', 0, '2026-06-25 17:32:23'),
(28, 3, 1, 'Truyện theo dõi có chương mới!', 'Bộ truyện \'One Piece\' bạn thích vừa ra mắt Chương 3.0. Khám phá ngay!', 0, '2026-06-25 17:32:23'),
(29, 3, 4, 'Truyện theo dõi có chương mới!', 'Bộ truyện \'Dragon Ball\' bạn thích vừa ra mắt Chương 3.0: rỹcghb. Khám phá ngay!', 0, '2026-06-25 17:36:52'),
(32, 2, 2, 'Truyện theo dõi có chương mới!', 'Bộ truyện \'Naruto\' bạn thích vừa ra mắt Chương 4.0. Khám phá ngay!', 0, '2026-06-28 01:19:38'),
(33, 6, 2, 'Truyện theo dõi có chương mới!', 'Bộ truyện \'Naruto\' bạn thích vừa ra mắt Chương 4.0. Khám phá ngay!', 1, '2026-06-28 01:19:38'),
(34, 2, 1, 'Chương truyện đã được chỉnh sửa', 'Chương 3.0 của bộ truyện \'One Piece\' vừa được Admin cập nhật lại nội dung mới.', 0, '2026-06-28 01:47:30'),
(35, 3, 1, 'Chương truyện đã được chỉnh sửa', 'Chương 3.0 của bộ truyện \'One Piece\' vừa được Admin cập nhật lại nội dung mới.', 0, '2026-06-28 01:47:30'),
(36, 3, 4, 'Cập nhật thông tin truyện', 'Truyện \'Dragon Ball\' bạn yêu thích vừa được cập nhật thông tin mới.', 0, '2026-06-28 09:37:24');

-- --------------------------------------------------------

--
-- Table structure for table `rating`
--

CREATE TABLE `rating` (
  `UserID` int(11) NOT NULL,
  `ComicID` int(11) NOT NULL,
  `Score` int(11) NOT NULL CHECK (`Score` >= 1 and `Score` <= 5),
  `CreatedAt` datetime DEFAULT current_timestamp(),
  `UpdatedAt` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `rating`
--

INSERT INTO `rating` (`UserID`, `ComicID`, `Score`, `CreatedAt`, `UpdatedAt`) VALUES
(2, 1, 5, '2026-06-12 01:28:42', '2026-06-12 01:28:42'),
(2, 2, 5, '2026-06-12 01:28:42', '2026-06-12 01:28:42'),
(3, 1, 4, '2026-06-12 01:28:42', '2026-06-12 01:28:42'),
(3, 4, 5, '2026-06-12 01:28:42', '2026-06-12 01:28:42'),
(4, 2, 4, '2026-06-12 01:28:42', '2026-06-12 01:28:42'),
(4, 3, 5, '2026-06-12 01:28:42', '2026-06-12 01:28:42'),
(6, 2, 4, '2026-06-14 02:18:27', '2026-06-14 02:18:34');

-- --------------------------------------------------------

--
-- Table structure for table `readinghistory`
--

CREATE TABLE `readinghistory` (
  `HistoryID` int(11) NOT NULL,
  `UserID` int(11) NOT NULL,
  `ComicID` int(11) NOT NULL,
  `LastChapterID` int(11) DEFAULT NULL,
  `LastPage` int(11) DEFAULT 0,
  `UpdatedAt` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `readinghistory`
--

INSERT INTO `readinghistory` (`HistoryID`, `UserID`, `ComicID`, `LastChapterID`, `LastPage`, `UpdatedAt`) VALUES
(1, 2, 1, 2, 10, '2026-06-12 01:28:42'),
(2, 2, 2, 4, 8, '2026-06-12 01:28:42'),
(3, 3, 4, 8, 15, '2026-06-12 01:28:42'),
(4, 4, 3, 6, 12, '2026-06-12 01:28:42'),
(5, 6, 2, 4, 0, '2026-06-28 09:48:21'),
(6, 6, 1, 2, 0, '2026-06-28 09:41:37'),
(7, 6, 4, 8, 0, '2026-06-28 09:30:42'),
(8, 6, 12, 18, 0, '2026-06-28 09:40:25'),
(9, 6, 8, 14, 0, '2026-06-28 09:40:57'),
(10, 6, 3, 6, 0, '2026-06-28 09:44:38');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `UserID` int(11) NOT NULL,
  `Email` varchar(255) NOT NULL,
  `PasswordHash` varchar(255) NOT NULL,
  `AvatarUrl` varchar(255) DEFAULT NULL,
  `DisplayName` varchar(255) NOT NULL,
  `Role` varchar(255) DEFAULT NULL,
  `Status` varchar(255) DEFAULT NULL,
  `CreatedAt` datetime DEFAULT current_timestamp(),
  `OtpCode` varchar(255) DEFAULT NULL,
  `OtpExpiry` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`UserID`, `Email`, `PasswordHash`, `AvatarUrl`, `DisplayName`, `Role`, `Status`, `CreatedAt`, `OtpCode`, `OtpExpiry`) VALUES
(1, 'admin@comic.com', 'hash_admin', NULL, 'Admin', 'Admin', 'Active', '2026-06-12 01:28:41', NULL, NULL),
(2, 'huy@gmail.com', 'hash_huy', NULL, 'Huy', 'User', 'Active', '2026-06-12 01:28:41', NULL, NULL),
(3, 'minh@gmail.com', 'hash_minh', NULL, 'Minh', 'User', 'Active', '2026-06-12 01:28:41', NULL, NULL),
(4, 'an@gmail.com', 'hash_an', NULL, 'An', 'User', NULL, '2026-06-12 01:28:41', NULL, NULL),
(5, 'yuhbui1725@gmail.com', '4JTpsLdrVwYGQsdbk6xzZ5I0CRbjOWqToKHSF2rHPiw=', 'http://localhost:8080/uploads/avatars/5_avatar_1781971595387.jpg', 'YuhBui', 'Admin', 'Active', '2026-06-12 01:30:47', NULL, NULL),
(6, 'huybui1725@gmail.com', '4JTpsLdrVwYGQsdbk6xzZ5I0CRbjOWqToKHSF2rHPiw=', 'http://localhost:8080/uploads/avatars/6_avatar_1781869186110.jpg', 'HuyBui', 'User', 'Active', '2026-06-12 01:31:01', '438713', '2026-06-26 18:55:58.000000');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `categories`
--
ALTER TABLE `categories`
  ADD PRIMARY KEY (`CategoryID`),
  ADD UNIQUE KEY `Name` (`Name`);

--
-- Indexes for table `chapterimages`
--
ALTER TABLE `chapterimages`
  ADD PRIMARY KEY (`ImageID`),
  ADD KEY `ChapterID` (`ChapterID`);

--
-- Indexes for table `chapters`
--
ALTER TABLE `chapters`
  ADD PRIMARY KEY (`ChapterID`),
  ADD KEY `ComicID` (`ComicID`);

--
-- Indexes for table `comics`
--
ALTER TABLE `comics`
  ADD PRIMARY KEY (`ComicID`);

--
-- Indexes for table `comic_categories`
--
ALTER TABLE `comic_categories`
  ADD PRIMARY KEY (`ComicID`,`CategoryID`),
  ADD KEY `CategoryID` (`CategoryID`);

--
-- Indexes for table `comments`
--
ALTER TABLE `comments`
  ADD PRIMARY KEY (`CommentID`),
  ADD KEY `UserID` (`UserID`),
  ADD KEY `ComicID` (`ComicID`),
  ADD KEY `ChapterID` (`ChapterID`),
  ADD KEY `ParentCommentID` (`ParentCommentID`);

--
-- Indexes for table `comment_interactions`
--
ALTER TABLE `comment_interactions`
  ADD PRIMARY KEY (`InteractionID`),
  ADD UNIQUE KEY `UserID` (`UserID`,`CommentID`),
  ADD KEY `CommentID` (`CommentID`);

--
-- Indexes for table `comment_reports`
--
ALTER TABLE `comment_reports`
  ADD PRIMARY KEY (`ReportID`),
  ADD KEY `UserID` (`UserID`),
  ADD KEY `CommentID` (`CommentID`);

--
-- Indexes for table `follows`
--
ALTER TABLE `follows`
  ADD PRIMARY KEY (`UserID`,`ComicID`),
  ADD KEY `ComicID` (`ComicID`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`NotificationID`),
  ADD KEY `UserID` (`UserID`),
  ADD KEY `ComicID` (`ComicID`);

--
-- Indexes for table `rating`
--
ALTER TABLE `rating`
  ADD PRIMARY KEY (`UserID`,`ComicID`),
  ADD KEY `ComicID` (`ComicID`);

--
-- Indexes for table `readinghistory`
--
ALTER TABLE `readinghistory`
  ADD PRIMARY KEY (`HistoryID`),
  ADD KEY `UserID` (`UserID`),
  ADD KEY `ComicID` (`ComicID`),
  ADD KEY `LastChapterID` (`LastChapterID`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`UserID`),
  ADD UNIQUE KEY `Email` (`Email`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `categories`
--
ALTER TABLE `categories`
  MODIFY `CategoryID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT for table `chapterimages`
--
ALTER TABLE `chapterimages`
  MODIFY `ImageID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=179;

--
-- AUTO_INCREMENT for table `chapters`
--
ALTER TABLE `chapters`
  MODIFY `ChapterID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- AUTO_INCREMENT for table `comics`
--
ALTER TABLE `comics`
  MODIFY `ComicID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT for table `comments`
--
ALTER TABLE `comments`
  MODIFY `CommentID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=94;

--
-- AUTO_INCREMENT for table `comment_interactions`
--
ALTER TABLE `comment_interactions`
  MODIFY `InteractionID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=32;

--
-- AUTO_INCREMENT for table `comment_reports`
--
ALTER TABLE `comment_reports`
  MODIFY `ReportID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `NotificationID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=37;

--
-- AUTO_INCREMENT for table `readinghistory`
--
ALTER TABLE `readinghistory`
  MODIFY `HistoryID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `UserID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `chapterimages`
--
ALTER TABLE `chapterimages`
  ADD CONSTRAINT `chapterimages_ibfk_1` FOREIGN KEY (`ChapterID`) REFERENCES `chapters` (`ChapterID`) ON DELETE CASCADE;

--
-- Constraints for table `chapters`
--
ALTER TABLE `chapters`
  ADD CONSTRAINT `chapters_ibfk_1` FOREIGN KEY (`ComicID`) REFERENCES `comics` (`ComicID`) ON DELETE CASCADE;

--
-- Constraints for table `comic_categories`
--
ALTER TABLE `comic_categories`
  ADD CONSTRAINT `comic_categories_ibfk_1` FOREIGN KEY (`ComicID`) REFERENCES `comics` (`ComicID`) ON DELETE CASCADE,
  ADD CONSTRAINT `comic_categories_ibfk_2` FOREIGN KEY (`CategoryID`) REFERENCES `categories` (`CategoryID`) ON DELETE CASCADE;

--
-- Constraints for table `comments`
--
ALTER TABLE `comments`
  ADD CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`) ON DELETE CASCADE,
  ADD CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`ComicID`) REFERENCES `comics` (`ComicID`) ON DELETE CASCADE,
  ADD CONSTRAINT `comments_ibfk_3` FOREIGN KEY (`ChapterID`) REFERENCES `chapters` (`ChapterID`) ON DELETE CASCADE,
  ADD CONSTRAINT `comments_ibfk_4` FOREIGN KEY (`ParentCommentID`) REFERENCES `comments` (`CommentID`) ON DELETE SET NULL;

--
-- Constraints for table `comment_interactions`
--
ALTER TABLE `comment_interactions`
  ADD CONSTRAINT `comment_interactions_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`) ON DELETE CASCADE,
  ADD CONSTRAINT `comment_interactions_ibfk_2` FOREIGN KEY (`CommentID`) REFERENCES `comments` (`CommentID`) ON DELETE CASCADE;

--
-- Constraints for table `comment_reports`
--
ALTER TABLE `comment_reports`
  ADD CONSTRAINT `comment_reports_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`) ON DELETE CASCADE,
  ADD CONSTRAINT `comment_reports_ibfk_2` FOREIGN KEY (`CommentID`) REFERENCES `comments` (`CommentID`) ON DELETE CASCADE;

--
-- Constraints for table `follows`
--
ALTER TABLE `follows`
  ADD CONSTRAINT `follows_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`) ON DELETE CASCADE,
  ADD CONSTRAINT `follows_ibfk_2` FOREIGN KEY (`ComicID`) REFERENCES `comics` (`ComicID`) ON DELETE CASCADE;

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`) ON DELETE CASCADE,
  ADD CONSTRAINT `notifications_ibfk_2` FOREIGN KEY (`ComicID`) REFERENCES `comics` (`ComicID`) ON DELETE SET NULL;

--
-- Constraints for table `rating`
--
ALTER TABLE `rating`
  ADD CONSTRAINT `rating_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`) ON DELETE CASCADE,
  ADD CONSTRAINT `rating_ibfk_2` FOREIGN KEY (`ComicID`) REFERENCES `comics` (`ComicID`) ON DELETE CASCADE;

--
-- Constraints for table `readinghistory`
--
ALTER TABLE `readinghistory`
  ADD CONSTRAINT `readinghistory_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`) ON DELETE CASCADE,
  ADD CONSTRAINT `readinghistory_ibfk_2` FOREIGN KEY (`ComicID`) REFERENCES `comics` (`ComicID`) ON DELETE CASCADE,
  ADD CONSTRAINT `readinghistory_ibfk_3` FOREIGN KEY (`LastChapterID`) REFERENCES `chapters` (`ChapterID`) ON DELETE SET NULL;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
