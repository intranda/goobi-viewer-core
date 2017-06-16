-- phpMyAdmin SQL Dump
-- version 3.4.10.1deb1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Erstellungszeit: 22. Feb 2013 um 16:03
-- Server Version: 5.5.29
-- PHP-Version: 5.3.10-1ubuntu3.5

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Datenbank: `viewer`
--

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `bookshelf_items`
--

CREATE TABLE IF NOT EXISTS `bookshelf_items` (
  `bookshelf_item_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date_added` date DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `logid` varchar(255) DEFAULT NULL,
  `main_title` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `pi` varchar(255) DEFAULT NULL,
  `urn` varchar(255) DEFAULT NULL,
  `bookshelf_id` bigint(20) NOT NULL,
  PRIMARY KEY (`bookshelf_item_id`),
  KEY `FK_bookshelf_items_bookshelf_id` (`bookshelf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `bookshelves`
--

CREATE TABLE IF NOT EXISTS `bookshelves` (
  `bookshelf_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `public` tinyint(1) DEFAULT '0',
  `name` varchar(255) NOT NULL,
  `owner_id` bigint(20) NOT NULL,
  PRIMARY KEY (`bookshelf_id`),
  KEY `FK_bookshelves_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `ip_ranges`
--

CREATE TABLE IF NOT EXISTS `ip_ranges` (
  `ip_range_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `subnet_mask` varchar(255) NOT NULL,
  PRIMARY KEY (`ip_range_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `licenses`
--

CREATE TABLE IF NOT EXISTS `licenses` (
  `license_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `conditions` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `date_end` datetime DEFAULT NULL,
  `date_start` datetime DEFAULT NULL,
  `ip_range_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `user_group_id` bigint(20) DEFAULT NULL,
  `license_type_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`license_id`),
  KEY `FK_licenses_user_group_id` (`user_group_id`),
  KEY `FK_licenses_ip_range_id` (`ip_range_id`),
  KEY `FK_licenses_license_type_id` (`license_type_id`),
  KEY `FK_licenses_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `license_privileges`
--

CREATE TABLE IF NOT EXISTS `license_privileges` (
  `license_id` bigint(20) DEFAULT NULL,
  `privilege_name` varchar(255) DEFAULT NULL,
  KEY `FK_license_privileges_license_id` (`license_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `license_types`
--

CREATE TABLE IF NOT EXISTS `license_types` (
  `license_type_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`license_type_id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `license_type_privileges`
--

CREATE TABLE IF NOT EXISTS `license_type_privileges` (
  `license_type_id` bigint(20) DEFAULT NULL,
  `privilege_name` varchar(255) DEFAULT NULL,
  KEY `FK_license_type_privileges_license_type_id` (`license_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `openid_accounts`
--

CREATE TABLE IF NOT EXISTS `openid_accounts` (
  `user_id` bigint(20) DEFAULT NULL,
  `claimed_identifier` varchar(255) DEFAULT NULL,
  KEY `FK_openid_accounts_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `roles`
--

CREATE TABLE IF NOT EXISTS `roles` (
  `role_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=2 ;

--
-- Daten für Tabelle `roles`
--

INSERT INTO `roles` (`role_id`, `description`, `name`) VALUES
(1, NULL, 'member');

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `role_privileges`
--

CREATE TABLE IF NOT EXISTS `role_privileges` (
  `role_id` bigint(20) DEFAULT NULL,
  `privilege_name` varchar(255) DEFAULT NULL,
  KEY `FK_role_privileges_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `users`
--

CREATE TABLE IF NOT EXISTS `users` (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `active` tinyint(1) DEFAULT '0',
  `comments` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_login` datetime DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `nickname` varchar(255) DEFAULT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `superuser` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `user_groups`
--

CREATE TABLE IF NOT EXISTS `user_groups` (
  `user_group_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `active` tinyint(1) DEFAULT '0',
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `owner_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`user_group_id`),
  KEY `FK_user_groups_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `user_role`
--

CREATE TABLE IF NOT EXISTS `user_role` (
  `user_role_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `user_group_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`user_role_id`),
  KEY `FK_user_role_role_id` (`role_id`),
  KEY `FK_user_role_user_id` (`user_id`),
  KEY `FK_user_role_user_group_id` (`user_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

--
-- Constraints der exportierten Tabellen
--

--
-- Constraints der Tabelle `bookshelf_items`
--
ALTER TABLE `bookshelf_items`
  ADD CONSTRAINT `FK_bookshelf_items_bookshelf_id` FOREIGN KEY (`bookshelf_id`) REFERENCES `bookshelves` (`bookshelf_id`);

--
-- Constraints der Tabelle `bookshelves`
--
ALTER TABLE `bookshelves`
  ADD CONSTRAINT `FK_bookshelves_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `users` (`user_id`);

--
-- Constraints der Tabelle `licenses`
--
ALTER TABLE `licenses`
  ADD CONSTRAINT `FK_licenses_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  ADD CONSTRAINT `FK_licenses_ip_range_id` FOREIGN KEY (`ip_range_id`) REFERENCES `ip_ranges` (`ip_range_id`),
  ADD CONSTRAINT `FK_licenses_license_type_id` FOREIGN KEY (`license_type_id`) REFERENCES `license_types` (`license_type_id`),
  ADD CONSTRAINT `FK_licenses_user_group_id` FOREIGN KEY (`user_group_id`) REFERENCES `user_groups` (`user_group_id`);

--
-- Constraints der Tabelle `license_privileges`
--
ALTER TABLE `license_privileges`
  ADD CONSTRAINT `FK_license_privileges_license_id` FOREIGN KEY (`license_id`) REFERENCES `licenses` (`license_id`);

--
-- Constraints der Tabelle `license_type_privileges`
--
ALTER TABLE `license_type_privileges`
  ADD CONSTRAINT `FK_license_type_privileges_license_type_id` FOREIGN KEY (`license_type_id`) REFERENCES `license_types` (`license_type_id`);

--
-- Constraints der Tabelle `openid_accounts`
--
ALTER TABLE `openid_accounts`
  ADD CONSTRAINT `FK_openid_accounts_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

--
-- Constraints der Tabelle `role_privileges`
--
ALTER TABLE `role_privileges`
  ADD CONSTRAINT `FK_role_privileges_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`);

--
-- Constraints der Tabelle `user_groups`
--
ALTER TABLE `user_groups`
  ADD CONSTRAINT `FK_user_groups_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `users` (`user_id`);

--
-- Constraints der Tabelle `user_role`
--
ALTER TABLE `user_role`
  ADD CONSTRAINT `FK_user_role_user_group_id` FOREIGN KEY (`user_group_id`) REFERENCES `user_groups` (`user_group_id`),
  ADD CONSTRAINT `FK_user_role_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`),
  ADD CONSTRAINT `FK_user_role_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
