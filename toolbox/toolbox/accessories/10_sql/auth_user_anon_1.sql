update auth_user_password set password = '$2a$08$0zyKvJ3lCOr3AonXQJYmcuqc092PvIrhONymcfmbjMGSCnt5PuhIK' where user_id in (select id from auth_user where username != 'admin'); -- 'alma'
update auth_user_password set password = '$2a$10$XxN0sMcfvgC1gCqVt.ARguVhX7XmA.eQlf2/gSByNdG2l0BqoHeGe' where user_id in (select id from auth_user where username = 'admin'); -- 'admin'

update auth_user_info set email = 'example@example.com';