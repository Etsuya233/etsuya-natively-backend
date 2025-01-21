package com.ety.natively.domain.dto;

import com.ety.natively.constant.PostContentType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 1. @JsonTypeInfo
 * @JsonTypeInfo 注解定义了多态类型信息的使用方式，包括：
 *
 * use = JsonTypeInfo.Id.NAME：表示用子类名称来区分类型。这里的子类名称是通过 @JsonSubTypes 中的 name 属性指定的（如 "1"、"2"）。
 * include = JsonTypeInfo.As.EXISTING_PROPERTY：表示 type 属性已经存在于 JSON 数据中，不需要额外添加或嵌套。
 * property = "type"：指定多态类型信息来源于 type 属性。例如，如果 JSON 数据是 { "type": "1", ... }，则 type 的值会用来决定反序列化成哪个子类。
 * visible = true：表示 type 字段在反序列化后依然可见。这样你可以在反序列化后的对象中直接获取 type 的值，而不需要再额外存储。
 * 2. @JsonSubTypes
 * @JsonSubTypes 注解定义了可能的子类映射，用来指定 type 值和子类之间的关系：
 *
 * 每个 @JsonSubTypes.Type 代表一种类型：
 * value：表示子类的 Java 类型。例如，TextTemplate.class 对应文本类型模板。
 * name：表示 JSON 中 type 字段的值。例如，"1" 表示这个数据对应 TextTemplate。
 * 3. 配置含义
 * 结合这两个注解，Spring Boot 会根据 type 字段的值自动识别需要反序列化到的具体子类。例如：
 *
 * 如果接收到的 JSON 数据为：
 * json
 * Copy code
 * {
 *   "type": "1",
 *   "value": "example text"
 * }
 * Jackson 会根据 type: "1" 知道这是一个 TextTemplate，然后把它反序列化成 TextTemplate 类的实例。
 * 4. 子类声明的关键
 * 子类需要有 type 字段，且类型与 JSON 数据中的字段类型一致。
 * 子类的字段应该与 JSON 数据匹配，例如 TextTemplate 类需要有 value 字段。
 */
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.EXISTING_PROPERTY,
		property = "type",
		visible = true
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = PostContentText.class, name = PostContentType.TEXT_STR),
		@JsonSubTypes.Type(value = PostContentCompare.class, name = PostContentType.COMPARE_STR),
		@JsonSubTypes.Type(value = PostContentImage.class, name = PostContentType.IMAGE_STR),
		@JsonSubTypes.Type(value = PostContentVoice.class, name = PostContentType.VOICE_STR),
		@JsonSubTypes.Type(value = PostContentMarkdown.class, name = PostContentType.MARKDOWN_STR)
})
public interface PostContentTemplate {
	Integer getType();
}
