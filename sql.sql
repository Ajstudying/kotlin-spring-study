use myapp2;
show processlist;

-- 아래는 두 문장씩 한문장임 -- 
select * from information_schema.tables
where table_schema = 'myapp2';
select * from information_schema.columns
where table_schema = 'myapp2';

CREATE TABLE `post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `content` text NOT NULL,
  `created_date` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- --

INSERT INTO `post` (`title`, `content`, `created_date`,profile_id) VALUES
('제목 1', '내용 1', '2023-09-08 10:00:00.000000', 1),
('제목 2', '내용 2', '2023-09-08 10:15:00.000000', 1),
('제목 3', '내용 3', '2023-09-08 10:30:00.000000', 1),
('제목 4', '내용 4', '2023-09-08 10:45:00.000000', 1),
('제목 5', '내용 5', '2023-09-08 11:00:00.000000', 1),
('제목 6', '내용 6', '2023-09-08 11:15:00.000000', 1),
('제목 7', '내용 7', '2023-09-08 11:30:00.000000', 1),
('제목 8', '내용 8', '2023-09-08 11:45:00.000000', 1),
('제목 9', '내용 9', '2023-09-08 12:00:00.000000', 1),
('제목 10', '내용 10', '2023-09-08 12:15:00.000000', 1);
-- ('제목 11', '내용 11', '2023-09-08 12:30:00.000000', 1),
-- ('제목 12', '내용 12', '2023-09-08 12:45:00.000000', 1),
-- ('제목 13', '내용 13', '2023-09-08 13:00:00.000000', 1),
-- ('제목 14', '내용 14', '2023-09-08 13:15:00.000000', 1),
-- ('제목 15', '내용 15', '2023-09-08 13:30:00.000000', 1),
-- ('제목 16', '내용 16', '2023-09-08 13:45:00.000000', 1),
-- ('제목 17', '내용 17', '2023-09-08 14:00:00.000000', 1),
-- ('제목 18', '내용 18', '2023-09-08 14:15:00.000000', 1),
-- ('제목 19', '내용 19', '2023-09-08 14:30:00.000000', 1),
-- ('제목 20', '내용 20', '2023-09-08 14:45:00.000000', 1),
-- ('제목 21', '내용 21', '2023-09-08 15:00:00.000000', 1),
-- ('제목 22', '내용 22', '2023-09-08 15:15:00.000000', 1),
-- ('제목 23', '내용 23', '2023-09-08 15:30:00.000000', 1),
-- ('제목 24', '내용 24', '2023-09-08 15:45:00.000000', 1),
-- ('제목 25', '내용 25', '2023-09-08 16:00:00.000000', 1),
-- ('제목 26', '내용 26', '2023-09-08 16:15:00.000000', 1),
-- ('제목 27', '내용 27', '2023-09-08 16:30:00.000000', 1),
-- ('제목 28', '내용 28', '2023-09-08 16:45:00.000000', 1),
-- ('제목 29', '내용 29', '2023-09-08 17:00:00.000000', 1),
-- ('제목 30', '내용 30', '2023-09-08 17:15:00.000000', 1);

select * from post;
select * from post_comment;

select * from post order by id desc;

select * from identity;
select * from profile;
truncate table post;
truncate table post_comment;

truncate table identity;
truncate table profile;


-- left join
-- 왼쪽 테이블을 필수적으로 있고, 오른쪽 테이블에는 없을 수도 있음.
-- 둘 다 있으면 inner join
select * from post p left join post_comment c on p.id = c.post_id;
-- ↑ post_id 랑 comment_id랑 서로 연결해서 조회를 하라는 의미.

-- select에는 그룹핑 열이 나와줘야 함
-- 그룹핑 열은 제외하고는 집계함수(count, sum, avg, max)↓ 몇개 있나~ 정도만 나옴.
-- c.id는 커맨트id
-- select p.id, count(c.id) as commentCount
select p.id, p.title, p.content, p.created_date, pf.nickname, count(c.id) as commentCount
from post p
    inner join profile pf on p.profile_id = pf.id 
    left join post_comment c on p.id = c.post_id
 -- post의 id값을 기준으로 그룹핑
-- group by p.id;
group by p.id, p.title, p.content, p.created_date, pf.nickname;

  
  INSERT INTO `post_comment` (`post_id`, `comment`, profile_id) VALUES
  (FLOOR(RAND() * 10) + 1, 'This is a great post.', 1),
  (FLOOR(RAND() * 10) + 1, 'I totally agree with your points.', 1),
  (FLOOR(RAND() * 10) + 1, 'Thanks for sharing this information.', 1),
  (FLOOR(RAND() * 10) + 1, 'I have a different perspective on this.', 1),
  (FLOOR(RAND() * 10) + 1, 'Can you provide more examples related to this?', 1),
  (FLOOR(RAND() * 10) + 1, 'I found a typo in the post.', 1),
  (FLOOR(RAND() * 10) + 1, 'Your insights are really valuable.', 1),
  (FLOOR(RAND() * 10) + 1, 'I''m looking forward to your next post.', 1),
  (FLOOR(RAND() * 10) + 1, 'This post needs more references.', 1),
  (FLOOR(RAND() * 10) + 1, 'I''m glad I stumbled upon this.', 1),
  (FLOOR(RAND() * 10) + 1, 'I have a question regarding point #2.', 1),
  (FLOOR(RAND() * 10) + 1, 'You explained this complex topic very well.', 1),
  (FLOOR(RAND() * 10) + 1, 'I''m sharing this with my friends for sure.', 1),
  (FLOOR(RAND() * 10) + 1, 'Looking forward to deeper insights in the future.', 1),
  (FLOOR(RAND() * 10) + 1, 'I''m bookmarking this page.', 1),
  (FLOOR(RAND() * 10) + 1, 'I wish there were more examples provided.', 1),
  (FLOOR(RAND() * 10) + 1, 'This post is hard to understand.', 1),
  (FLOOR(RAND() * 10) + 1, 'Your points are well-researched.', 1),
  (FLOOR(RAND() * 10) + 1, 'I''m sharing this on social media.', 1),
  (FLOOR(RAND() * 10) + 1, 'Can you recommend more resources on this topic?', 1),
  (FLOOR(RAND() * 10) + 1, 'I have a similar experience to share.', 1),
  (FLOOR(RAND() * 10) + 1, 'I disagree with some parts of the post.', 1),
  (FLOOR(RAND() * 10) + 1, 'Your writing style is engaging.', 1),
  (FLOOR(RAND() * 10) + 1, 'I''m looking forward to more real-life examples.', 1),
  (FLOOR(RAND() * 10) + 1, 'This post changed my viewpoint.', 1),
  (FLOOR(RAND() * 10) + 1, 'I''m recommending this to my colleagues.', 1),
  (FLOOR(RAND() * 10) + 1, 'Your analysis is spot-on.', 1),
  (FLOOR(RAND() * 10) + 1, 'I''m eager to learn more from you.', 1),
  (FLOOR(RAND() * 10) + 1, 'This post is a bit too technical for me.', 1),
  (FLOOR(RAND() * 10) + 1, 'I appreciate the effort you put into this content.', 1);