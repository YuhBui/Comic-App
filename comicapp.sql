-- 1. Bảng Người dùng
CREATE TABLE Users (
    UserID INT AUTO_INCREMENT PRIMARY KEY,
    Email VARCHAR(255) NOT NULL UNIQUE,
    PasswordHash VARCHAR(255) NOT NULL,
    AvatarUrl VARCHAR(500),
    DisplayName VARCHAR(100) NOT NULL,
    Role VARCHAR(20) DEFAULT 'User', -- 'User' hoặc 'Admin'
    Status VARCHAR(20) DEFAULT 'Active',
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng Thể loại
CREATE TABLE Categories (
    CategoryID INT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL UNIQUE
);

-- 3. Bảng Truyện
CREATE TABLE Comics (
    ComicID INT AUTO_INCREMENT PRIMARY KEY,
    Title VARCHAR(255) NOT NULL,
    Author VARCHAR(255),
    Description TEXT,
    CoverImageUrl VARCHAR(500),
    ViewCount INT DEFAULT 0,
    Rating FLOAT DEFAULT 0.0,
    Status VARCHAR(50) DEFAULT 'Ongoing',
    IsHidden BOOLEAN DEFAULT FALSE,
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng Trung gian Truyện - Thể loại
CREATE TABLE Comic_Categories (
    ComicID INT,
    CategoryID INT,
    PRIMARY KEY (ComicID, CategoryID),
    FOREIGN KEY (ComicID) REFERENCES Comics(ComicID) ON DELETE CASCADE,
    FOREIGN KEY (CategoryID) REFERENCES Categories(CategoryID) ON DELETE CASCADE
);

-- 5. Bảng Đánh giá Truyện (Rating)
CREATE TABLE Rating (
    UserID INT,
    ComicID INT,
    Score INT NOT NULL CHECK (Score >= 1 AND Score <= 5),
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (UserID, ComicID),
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (ComicID) REFERENCES Comics(ComicID) ON DELETE CASCADE
);

-- 6. Bảng Theo dõi truyện
CREATE TABLE Follows (
    UserID INT,
    ComicID INT,
    IsNotificationOn BOOLEAN DEFAULT TRUE,
    FollowedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (UserID, ComicID),
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (ComicID) REFERENCES Comics(ComicID) ON DELETE CASCADE
);

-- 7. Bảng Chương truyện
CREATE TABLE Chapters (
    ChapterID INT AUTO_INCREMENT PRIMARY KEY,
    ComicID INT NOT NULL,
    ChapterNumber FLOAT NOT NULL,
    Title VARCHAR(255),
    ViewCount INT DEFAULT 0,
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ComicID) REFERENCES Comics(ComicID) ON DELETE CASCADE
);

-- 8. Bảng Ảnh của Chương
CREATE TABLE ChapterImages (
    ImageID INT AUTO_INCREMENT PRIMARY KEY,
    ChapterID INT NOT NULL,
    ImageUrl VARCHAR(500) NOT NULL,
    PageNumber INT NOT NULL,
    FOREIGN KEY (ChapterID) REFERENCES Chapters(ChapterID) ON DELETE CASCADE
);

-- 9. Bảng Lịch sử đọc (Lưu trên server để đồng bộ đa thiết bị)
CREATE TABLE ReadingHistory (
    HistoryID INT AUTO_INCREMENT PRIMARY KEY,
    UserID INT NOT NULL,
    ComicID INT NOT NULL,
    LastChapterID INT,
    LastPage INT DEFAULT 0,
    UpdatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (ComicID) REFERENCES Comics(ComicID) ON DELETE CASCADE,
    FOREIGN KEY (LastChapterID) REFERENCES Chapters(ChapterID) ON DELETE SET NULL
);

-- 10. Bảng Thông báo
CREATE TABLE Notifications (
    NotificationID INT AUTO_INCREMENT PRIMARY KEY,
    UserID INT NOT NULL,
    ComicID INT NULL,
    Title VARCHAR(255) NOT NULL,
    Message TEXT NOT NULL,
    IsRead BOOLEAN DEFAULT FALSE,
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (ComicID) REFERENCES Comics(ComicID) ON DELETE SET NULL
);

-- 11. Bảng Bình luận (Đã cập nhật các cột đếm số lượng)
CREATE TABLE Comments (
    CommentID INT AUTO_INCREMENT PRIMARY KEY,
    UserID INT NOT NULL,
    ComicID INT,
    ChapterID INT,
    ParentCommentID INT, -- Để làm tính năng Trả lời (Reply) bình luận
    Content TEXT NOT NULL,
    ReplyCount INT DEFAULT 0,
    LikeCount INT DEFAULT 0,
    DislikeCount INT DEFAULT 0,
    ReportCount INT DEFAULT 0,
    IsDeleted BOOLEAN DEFAULT FALSE,
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (ComicID) REFERENCES Comics(ComicID) ON DELETE CASCADE,
    FOREIGN KEY (ChapterID) REFERENCES Chapters(ChapterID) ON DELETE CASCADE,
    FOREIGN KEY (ParentCommentID) REFERENCES Comments(CommentID) ON DELETE SET NULL
);

-- 12. Bảng Tương tác Bình luận (Lưu chi tiết ai Like/Dislike)
CREATE TABLE Comment_Interactions (
    InteractionID INT AUTO_INCREMENT PRIMARY KEY,
    UserID INT NOT NULL,
    CommentID INT NOT NULL,
    InteractionType INT NOT NULL, -- 1: Like, -1: Dislike
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (CommentID) REFERENCES Comments(CommentID) ON DELETE CASCADE,
    UNIQUE (UserID, CommentID)
);

-- 13. Bảng Báo cáo Bình luận
CREATE TABLE Comment_Reports (
    ReportID INT AUTO_INCREMENT PRIMARY KEY,
    UserID INT NOT NULL,
    CommentID INT NOT NULL,
    Reason VARCHAR(255) NOT NULL,
    IsResolved BOOLEAN DEFAULT FALSE,
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (CommentID) REFERENCES Comments(CommentID) ON DELETE CASCADE
);
