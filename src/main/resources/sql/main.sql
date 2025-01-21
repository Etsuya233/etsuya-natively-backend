create database natively;

use natively;

show global variables like '%time%zone%';

create table user (
    id bigint primary key not null comment '用户ID',
    username varchar(20) unique not null comment '用户名',
    nickname varchar(64) not null comment '昵称',
    password varchar(255) not null comment '密码',
    email varchar(255) unique not null comment '邮箱',
    gender int comment '性别 0 女 1 男 2 其他',
    location char(2) comment '国家或地区 ISO 3166-1',
    timezone varchar(32) comment 'IANA时区',
    avatar varchar(255) comment '头像',
    status int default 1 comment '状态',
    following int default 0 not null comment '关注数目',
    followers int default 0 not null comment '粉丝',
    version tinyint default 0 comment '用于强制用户下线等',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table user_language (
    id bigint primary key not null comment '用户语言ID',
    user_id bigint not null comment '用户ID',
    lang varchar(5) not null comment 'ISO',
    native boolean not null default false comment '是否是母语',
    proficiency int not null default 3 comment '精通程度'
);

create table user_oauth (
    id bigint primary key not null comment '用户OAuthID',
    user_id bigint comment '用户ID',
    owner varchar(32) not null comment '提供商如google',
    owner_id varchar(255) not null comment 'OAuth的ID',
    registered boolean not null default false comment '是否已完成注册流程',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table ai_model (
      id int primary key not null comment 'AI大模型ID',
      name varchar(64) not null comment 'AI扮演的角色名称',
      model_name varchar(64) not null comment 'AI大模型名称',
      description varchar(64) comment '描述',
      prompt text not null comment '提示Prompt',
      generate_title boolean not null default false comment '是否手动生成标题',
      attach_count int not null default 0 comment '携带的历史消息数',
      temperature double comment '参数 temperature',
      top_p double comment '参数 top_p',
      max_tokens int comment '参数 max_tokens',
      presence_penalty double comment '参数 presence_penalty',
      frequency_penalty double comment '参数 frequency_penalty',
      create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
      update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table ai_record (
    id bigint not null primary key comment 'ID',
    user_id bigint not null comment '用户ID',
    model_id int not null comment '模型ID',
    conversation boolean not null default true comment '是否是与AI聊天的记录',
    message text not null comment '用户发送信息',
    result text not null comment 'AI返回信息',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table check_in (
    id bigint primary key not null comment '月签到记录ID',
    user_id bigint not null comment '用户ID',
    date char(6) not null comment '时间：YYYYmm',
    record int not null default 0 comment '签到记录，格式：第31位二进制位为第一天',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    unique(user_id, date)
);

create table achievement (
    id int primary key auto_increment not null comment '成就ID',
    name varchar(255) not null comment '成就名',
    description varchar(255) not null comment '成就描述',
    icon varchar(8) comment '成就符号',
    image varchar(255) comment '成就图像 和 符号二选一',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table user_achievement (
    id bigint primary key not null comment '用户成就ID',
    user_id bigint not null comment '用户ID',
    achievement_id int not null comment '成就ID',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    unique (user_id, achievement_id)
);

create table help_type (
    id int primary key auto_increment comment '帮助类型ID',
    icon varchar(8) comment '帮助符号',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC'
);

create table help_type_translation (
    id int primary key auto_increment comment '帮助类型翻译ID',
    help_type_id int not null comment '帮助类型ID',
    language varchar(5) not null comment '语言',
    translation varchar(255) not null comment '类型翻译',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table help (
    id int primary key auto_increment comment '帮助ID',
    type int not null comment '帮助类型',
    title varchar(255) not null comment '帮助标题',
    content text not null comment '帮助内容',
    language varchar(5) not null comment '帮助语言',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table location (
    id int primary key not null,
    name varchar(255) not null ,
    code char(2) not null
);

create table location_translation (
    id int primary key not null auto_increment,
    location_id int,
    lang varchar(5),
    translation varchar(255)
);

create table post (
    id bigint primary key not null comment '帖子ID',
    user_id bigint not null comment '用户ID',
    title varchar(255) comment '标题',
    content text comment '内容',
    type tinyint not null default 1 comment '类型，普通1，问答2',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table post_language (
    post_id bigint not null ,
    lang varchar(5) not null ,
    primary key (post_id, lang)
);

create table comment (
    id bigint primary key not null comment '评论ID',
    user_id bigint not null comment '用户ID',
    post_id bigint not null comment '帖子ID',
    parent_id bigint comment '关联评论ID',
    content text comment '内容',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table vote (
    id bigint primary key not null comment '投票ID',
    user_id bigint not null comment '投票人ID',
    post_id bigint comment '帖子ID',
    comment_id bigint comment '评论ID',
    type tinyint comment '1 点赞 -1 down vote' not null default 1,
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间',
    unique (user_id, post_id),
    unique (post_id, comment_id)
);

create table post_summary (
    post_id bigint primary key not null comment '帖子ID',
    upvote_count bigint not null comment '点赞数' default 0,
    downvote_count bigint not null comment '差评数' default 0,
    comment_count bigint not null default 0 comment '评论总数'
);

create table comment_summary (
    comment_id bigint primary key not null comment '帖子ID',
    upvote_count bigint not null comment '点赞数' default 0,
    downvote_count bigint not null comment '差评数' default 0,
    comment_count bigint not null default 0 comment '评论总数'
);

create table attachment (
    id bigint primary key not null comment '附件ID',
    post_id bigint comment '帖子ID',  -- 可为空，用于表示此附件属于一个帖子
    comment_id bigint comment '评论ID',  -- 可为空，用于表示此附件属于一个评论
    path varchar(512) not null comment '文件路径',  -- 附件的存储路径或URL
    type tinyint not null comment '文件类型',  -- 例如 "1 图片 2 音频",
    no int not null comment '第几个附件',
    size bigint not null comment '文件大小（字节）',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC'
);

create table bookmark (
    id bigint primary key not null comment '书签ID',
    reference_id bigint not null comment '对应收藏内容的ID',
    user_id bigint not null comment '用户ID',
    type tinyint not null default 1 comment '1，贴子 2，评论',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间',
    unique(reference_id, user_id, type)
);

create table chat_message (
    id bigint primary key not null comment '消息ID',
    sender_id bigint not null comment '发送者ID',
    receiver_id bigint not null comment '接受者ID',
    type int not null default 1 comment '消息类型（1，文本；2，图片；3，其他）',
    content text comment '消息内容',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table contact (
    id bigint primary key not null comment '关系ID',
    user_a_id bigint not null,
    user_b_id bigint not null,
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间'
);

create table conversation (
    user_a_id bigint not null,
    user_b_id bigint not null,
    last_id bigint comment '最后一条消息ID',
    create_time datetime(3) default current_timestamp(3) comment '创建时间UTC',
    update_time datetime(3) default current_timestamp(3) on update current_timestamp(3) comment '更新时间',
    primary key (user_a_id, user_b_id)
);

create table chat_unread (
    sender_id bigint not null,
    receiver_id bigint not null,
    count int default 0,
    unique (sender_id, receiver_id)
);