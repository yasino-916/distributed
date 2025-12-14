-- Add reviewer account to admins table
INSERT INTO admins (username, password, full_name) 
VALUES ('reviewer', 'pass123', 'Exam Reviewer')
ON DUPLICATE KEY UPDATE password='pass123', full_name='Exam Reviewer';
