create database natively;

use natively;

create table user (
    id bigint primary key not null comment '用户ID',
    username varchar(20) unique not null comment '用户名',
    nickname varchar(20) not null comment '昵称',
    password varchar(255) not null comment '密码',
    email varchar(255) unique not null comment '邮箱',
    gender int comment '性别 0 女 1 男 2 其他',
    location char(2) comment '国家或地区',
    timezone tinyint comment '时区，默认以创建时的国家与地区为准',
    avatar varchar(255) comment '头像',
    status int default 1 comment '状态',
    version tinyint default 0 comment '用于强制用户下线等',
    create_time bigint default 0
);

create table ai_record (
    id bigint not null primary key comment 'ID',
    user_id bigint not null comment '用户ID',
    type int not null comment '消息类型，比如优化表达什么的',
    conversation boolean not null default true comment '是否是与AI聊天的记录',
    message text not null comment '用户发送信息',
    result text not null comment 'AI返回信息',
    create_time bigint default 0
);

create table check_in (
    id bigint primary key not null comment '月签到记录ID',
    user_id bigint not null comment '用户ID',
    date char(6) not null comment '时间：YYYYmm',
    record int not null default 0 comment '签到记录，格式：第31位二进制位为第一天',
    unique(user_id, date)
);

create table achievement (
    id int primary key auto_increment not null comment '成就ID',
    name varchar(255) not null comment '成就名',
    description varchar(255) not null comment '成就描述',
    icon varchar(8) comment '成就符号',
    image varchar(255) comment '成就图像 和 符号二选一'
);

create table user_achievement (
    id bigint primary key not null comment '用户成就ID',
    user_id bigint not null comment '用户ID',
    achievement_id int not null comment '成就ID',
    create_time bigint default 0,
    unique (user_id, achievement_id)
);

create table help_type (
    id int primary key auto_increment comment '帮助类型ID',
    icon varchar(8) comment '帮助符号'
);

create table help_type_translation (
    id int primary key auto_increment comment '帮助类型翻译ID',
    help_type_id int not null comment '帮助类型ID',
    language varchar(5) not null comment '语言',
    translation varchar(255) not null comment '类型翻译'
);

create table help (
    id int primary key auto_increment comment '帮助ID',
    type int not null comment '帮助类型',
    title varchar(255) not null comment '帮助标题',
    content text not null comment '帮助内容',
    language varchar(5) not null comment '帮助语言',
    create_time bigint default 0
);