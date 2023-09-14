import org.jetbrains.kotlin.gradle.tasks.KotlinCompile



plugins {
	id("org.springframework.boot") version "3.1.3"
	id("io.spring.dependency-management") version "1.1.3"
	kotlin("jvm") version "1.8.22"
	kotlin("plugin.spring") version "1.8.22"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

val exposedVersion: String by project
dependencies {

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	runtimeOnly("com.mysql:mysql-connector-j")
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	// https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

	//spring-data-jdbc 가 있어야 데이터베이스에 연결이 됨. application properties는 연결 소스(설정)만 적어놓은 것!
	//아래의 것이 있어야 실행이 되는데 spring에는 기본적으로 깔려 있어서(내장 되어있어서) 가능한 것임.
	// https://mvnrepository.com/artifact/org.springframework.data/spring-data-jdbc
	//  implementation("org.springframework.data:spring-data-jdbc:3.1.3")
	implementation ("com.fasterxml.jackson.core:jackson-databind")

	//expose 의존성
	implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

	implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

	implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-money:$exposedVersion")

	//spring-data-jdbc 내장, 데이터소스 객체 생성 자동화 이거는 됨
	//spring transaction 호환, @Transation 이거는 현재 호환이 잘 안됨.
	//스프링 트랜잭션 매니저를 안 써야 더 디테일한 처리를 할 수 있음.
	implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")

	implementation("com.auth0:java-jwt:4.4.0")
	implementation("at.favre.lib:bcrypt:0.10.2")

}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
