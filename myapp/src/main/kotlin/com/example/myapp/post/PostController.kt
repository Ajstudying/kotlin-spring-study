package com.example.myapp.post

import com.example.myapp.auth.Auth
import com.example.myapp.auth.AuthProfile
import com.example.myapp.auth.Profiles
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.core.io.ResourceLoader
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.time.LocalDateTime
import java.util.*

//query-model, view-model
//domain-model(JPA entity)
//data class PostResponse(val id : Long, val title : String, val content: String, val createdDate: String)

@RestController
@RequestMapping("posts")
class PostController (private val resourceLoader: ResourceLoader) {

    private val POST_FILE_PATH = "files/post";
    // exposed selectAll -> List<ResultRow>
    // ResultRow는 transaction {} 구문 밖에서 접근 불가능함
    //transaction 구분 외부로 보낼 때는 별도의 객체로 변환해서 내보낸다.
    // 결과값: List<PostResponse>
    @Auth
    @GetMapping
    fun fetch() = transaction(){
        Posts.selectAll().map { r -> PostResponse(
                r[Posts.id], r[Posts.title], r[Posts.content], r[Posts.createdDate].toString()
        ) }
    }

    @GetMapping("/paging")
    fun paging(@RequestParam size: Int, @RequestParam page: Int)
            : Page<PostResponse> = transaction(Connection.TRANSACTION_READ_COMMITTED, readOnly = true)
    {
        //READ_COMMITTED, REPEATABLE_READ
        //전체 조회 중 50건 정도
        //누군가가 50건 중에 수정중이거나, 삭제중이거나
        // SELECT 잠시 wait

        //Mission Critical 서비스
        //금융권, 의료, 제조... 데이터 정확 잘 맞아야 되는 곳

        //TRANSACTION_READ_COMMITTED
        //insert/update/read 트랜젝션 시작
        //트랜젝션이 커밋이 안되어도 조회가 가능.
        //insert/update/delete 트랜젝션 상관없이 조회가능.

        //페이징 조회
        //object의 이름을 짧은 걸로 변경
        val p = Posts; //table alias
        //페이징 조회
        val content = Posts.selectAll().orderBy(Posts.id to SortOrder.DESC) //vararg 구조라 여러개의 순서 조건을 넣을 수 있음
            //Posts.selectAll().orderBy(Posts.id to SortOrder.DESC, Posts.createdDate to SortOrder.DESC)
            .limit(size, offset = (size * page).toLong()) //limit의 size는 Int인데, offset은 Long이어서 이렇게 형변환 해줘야 함.
            .map{ //r은 ResultRow
                    r -> PostResponse(r[p.id], r[p.title], r[p.content], r[p.createdDate].toString())
            }
        //전체 결과 카운트(페이징의 조건식이 같아야 함. 아니면 매개변수로 넘기면 됨..?)
        val totalCount = Posts.selectAll().count()
        return@transaction PageImpl(
            content, //List<PostResponse> (컬렉션)
            PageRequest.of(page, size), //Pageable
            totalCount) //전체 건수


        //작성시에 아래 문구를 먼저 넣어서 오류를 없앤 후 적어야 어느 부분이 잘못되었는지 확인하면서
        //코드를 쓰는게 가능하다.
//        return@transaction PageImpl(listOf())
    }

    //    /paging/search?size=10&page=0
    //    /paging/search?size=10&page=0&keyword="제목"
    @GetMapping("/paging/search")
    fun searchPaging(
        @RequestParam size : Int, @RequestParam page : Int, @RequestParam keyword : String?)
    : Page<PostResponse>
    = transaction(Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true) {
        //검색 조건 생성
        val query = when {
            keyword != null -> Posts.select {
                (Posts.title like "%${keyword}%") or
                        (Posts.content like "%${keyword}%" ) }
            else -> Posts.selectAll()
        }
        // 전체 결과 카운트
        val totalCount = query.count()
        // 페이징 조회
        val content = query
            .orderBy(Posts.id to SortOrder.DESC)
            .limit(size, offset= (size * page).toLong())
            .map { r ->
                PostResponse(r[Posts.id],
                    r[Posts.title],
                    r[Posts.content], r[Posts.createdDate].toString())
            }
        // Page 객체로 리턴
        return@transaction PageImpl(content, PageRequest.of(page, size),  totalCount)
    }

    @GetMapping("/commentCount")
    fun fetchCommentCount(@RequestParam size : Int, @RequestParam page : Int,
                          @RequestParam keyword : String?) : Page<PostCommentCountResponse>
            = transaction(Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true) {

//        -- select에는 그룹핑 열이 나와줘야 함
//        -- 그룹핑 열은 제외하고는 집계함수(count, sum, avg, max)
//        select p.id, p.title, p.content, p.created_date, pf.nickname, count(c.id) as commentCount
//        from post p
//        inner join profile pf on p.profile_id = pf.id
//                left join post_comment c on p.id = c.post_id
//                -- post의 id값을 기준으로 그룹핑
//                -- group by p.id;
//        group by p.id, p.title, p.content, p.created_date, pf.nickname;


        // 단축 이름 변수 사용
        val pf = Profiles;
        val p = Posts;
        val c = PostComments;

        //집계함수식의 별칭 설정
        val commentCount = PostComments.id.count(); //count(c.id) as commentCount

        //이렇게 써야 하기 때문에 별칭 등을 사용하게 되는 것.
//        ((Posts innerJoin Profiles) leftJoin PostComments)
//            .slice(p.id,  p.title, p.createdDate, p.profileId, pf.nickname, PostComments.id.count())
//            .selectAll()
//            .groupBy(p.id,  p.title, p.createdDate, p.profileId, pf.nickname)
//            .orderBy(Posts.id to SortOrder.DESC)
//            .limit(size, offset = (size * page).toLong())

        // 조인 및 특정 컬럼 선택 및 count함수 사용
        val slices = ((p innerJoin pf) leftJoin c)
            .slice(p.id, p.title, p.createdDate, p.profileId, pf.nickname, commentCount);

        // 검색 조건 설정
        val query = when {
            keyword != null -> slices.select((Posts.title like "%${keyword}%") or (Posts.content like "%${keyword}%" ))
            else -> slices.selectAll()
        }

        // 전체 결과 카운트 => 페이징 보다 앞 쪽에서 쳐야 함.
        val totalCount = query.count();

        // 페이징 조회
        val content = query
            .groupBy(p.id, p.title, p.createdDate, p.profileId, pf.nickname)
            .orderBy(p.id to SortOrder.DESC)
            .limit(size, offset= (size * page).toLong())
            .map { r -> PostCommentCountResponse(
                r[p.id],  r[p.title], r[p.createdDate].toString(),
                r[p.profileId].value, r[pf.nickname], r[commentCount]) } //위쪽에 슬라이스한 애들과 같아야 함. 없으면 에러생김

        // Page 객체로 리턴
        return@transaction PageImpl(content, PageRequest.of(page, size), totalCount);
    }

    //PostCreateRequest
    //title: String, content: String -> 둘다 null이 불가능
    //null체크 할 필요가 없음.
    @Auth
    @PostMapping
    fun create(@RequestBody request : PostCreateRequest, @RequestAttribute authProfile : AuthProfile)
    : ResponseEntity<Map<String, Any?>> {
        println("${request.title}, ${request.content}")

        //자바
        // Java: Object, class들의 최상위 클래스
        // Map<String, Object>
        //Object: nullable, int/long primitive 타입은 안됨, Integer, Long으로 써야 함.

        //코틀린
        //Map<String, Any?>
        //{"key" to null}
        //{"key" to obj}
        //{"key" to "str"}
        //{"key" to 0L} -> Map<String, Long>

        if(!request.validate()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "title and content fields are required"))
        }
        //not -nullable이기 때문에 null체크를 안해도 된다.
//        if(request.title.isEmpty() || request.content.isEmpty()){
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                .body(mapOf("error" to "title and content fields are required"))
//        }

        //Pair(first, second)
        //Student(name, age, grade) -> componentN() 메서드로 필드 순서를 지정했을 때만 쓸 수 있음.
        //데이터 클래스의 주 생성자에 들어있는 프로퍼티에 대해서는 컴파일러가 자동으로 componentN 함수를 만들어줍니다.
        //코틀린의 구조분해는 객체의 필드 순서가 정의 되어 있을 때만 사용 가능.
        //아래를 val(r, data)이런 식으로 써도 됨.
        val (result, response) = transaction {
            //insert 구문
            //List<ResultRow>?
            //null이 아닐 때만 result로 들어가기 때문에 List<ResultRow> 타입이 이것
            val result = Posts.insert {
                //매개변수 1개의 lambda함수의 매개변수를 it으로 단축 표기
                //함수식
                it[title] = request.title
//                it[Posts.title] = request.title 이 두개도 가능
//                it[this.title] = request.title
                it[content] = request.content
                it[createdDate] = LocalDateTime.now()
                it[profileId] = authProfile.id
            }.resultedValues //null일 수도 있기 때문에 List<ResultRow>?
                ?:  // null인 경우 이 구문이 실행됨.
                //Pair 타입 두개를 반환할 수 있는 클래스타입
                //ex) Pair(결과타입, 결과객체)
                //Pairs(first, second)
                return@transaction Pair(false, null)

            // ?: -> 앞의 식의 결과가 null이 아니면 앞의 식을 실행, null이면 뒤의 것을 실행
            //if(null 일때){} 식을 썼을 때 코틀린은 else문을 꼭 써야 하기 때문에 null체크 할 경우 elvis연산으로 바뀌게 됨.

            //List<ResultRow> -> ResultRow
            val record = result.first()
            //ResultRow -> Response
            return@transaction Pair(true, PostResponse(
                record[Posts.id],
                record[Posts.title],
                record[Posts.content],
                record[Posts.createdDate].toString(),
                ))
        }

        //정확히 insert 됐을 때
        if(result){
            return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("data" to response))
        }

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(mapOf("data" to response, "error" to "conflict")) //"data" to null도 됨.
//                ResponseEntity.created("http://localhost:8080/posts/1")
    }


    @Auth
    @DeleteMapping("/{id}")
    fun remove(@PathVariable id : Long, @RequestAttribute authProfile: AuthProfile) : ResponseEntity<Any> {
        //해당 id의 레코드 있는지 확인
        // 조회 결과를 쓰지 않고 있는지 없는지만 판단.
        //select + where 구문을 쓴다.

        //exposed
        // Posts.select{Posts.id.eq id} - eq(equal임)
        //Posts.select(where = {Posts.id eq id}).firstOrNull()
        //반환값: ResultRow?

        //SQL
        //select * from post where id = :id -> ResultRow?
        //[select * from] post [where] id = :id -> ResultRow?
        // post id = :id
        // Posts.slice(컬럼선택).select{조건문}.limit(..).groupBy(..).orderBy(..)
        //transaction { Posts.select(where: Posts.id eq id) <원래 이렇게 인데, 항상 쓰는 방식이라 아래처럼 줄임.
        transaction { Posts.select(where = (Posts.id eq id) and (Posts.profileId eq authProfile.id)).firstOrNull() }
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        //delete
        transaction {
            Posts.deleteWhere { Posts.id eq id }
        }

        //200 OK
        return ResponseEntity.ok().build()
    }

    @Auth
    @PutMapping("/{id}")
    fun modify(@PathVariable id : Long, @RequestBody request: PostModifyRequest, @RequestAttribute authProfile: AuthProfile): ResponseEntity<Any> {
        // 둘다 널이거나 빈값이면 400 : Bad request
        if(request.title.isNullOrEmpty() && request.content.isNullOrEmpty()) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "title or content are required"))
        }
        // id에 해당 레코드가 없으면 404
        transaction { Posts.select{ (Posts.id eq id) and (Posts.profileId eq authProfile.id)  }.firstOrNull() }
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        transaction {
            Posts.update({Posts.id eq id}) {
                //title이 null 또는 "" 아니면
                //값이 존재하면 수정
                if(!request.title.isNullOrEmpty()){
                    it[title] = request.title
                }
                if(!request.content.isNullOrEmpty()){
                    it[content] = request.content
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    //POST/posts/with-file
    //content-type: multipart/form-data
    //body: files=[...octet-stream]
    //      title=제목입니다.&content=내용입니다.
    //default 파일용량이 10mb이기 때문에 application properties에 따로 크기 설정 해줘야 함.
    @PostMapping("/with-file")
    fun createWithFile(@RequestParam files: Array<MultipartFile>, @RequestParam title: String, @RequestParam content: String)
    : ResponseEntity<PostWithFileResponse> {

        println("title: $title")
        println("content: $content")


       //files/post
        val dirPath = Paths.get(POST_FILE_PATH)
        if (!Files.exists(dirPath)) {
            //폴더 생성
            Files.createDirectories(dirPath)
        }
        val filesList = mutableListOf<Map<String, String?>>()

        //runBlocking, launch 코루틴 처리
        runBlocking {
            files.forEach {
                launch {
                    println("filename: ${it.originalFilename}")
                    //files/post/1.png

                    //{it.originalFilename!! 자바라이브러리라 string이 nullable이라 !!(null이 아니다)를 붙여줘야 함.
                    //uuid: 랜덤하게 문자열로 id값을 만들 수 있는 방법(순서X)
                    val uuidFileName = "${ UUID.randomUUID().toString() }.${ it.originalFilename!!.split(".").last() }"
                    val filePath = dirPath.resolve(uuidFileName)
                    //파일객체에서 스트림객체를 얻어옴
                    //스트림객체: 바이트배열 처리할 수 있는 객체
                    it.inputStream.use {
                        Files.copy(it, filePath, StandardCopyOption.REPLACE_EXISTING)
                    }
                    //파일의 메타데이터를 리스트-맵에 임시저장
                    filesList.add(mapOf("uuidFileName" to uuidFileName,
                        "contentType" to it.contentType,
                        "originalFileName" to it.originalFilename))
                }
            }
        }


        //데이터베이스에 파일정보와 데이터를 저장하고
        //post insert, post_file insert batch
        val result = transaction {
            val p = Posts
            val pf = PostFiles

            //포스트 1건 저장
            val insertedPost = p.insert {
                it[this.title] = title
                it[this.content] = content
                it[this.profileId] = 1 //강제로 1번 유저로(인증처리 전이라)
                it[this.createdDate] = LocalDateTime.now()
            }.resultedValues!!.first()

            //insert into....values(..),(..),(..);
            //참고로 batchInsert 를 하면 db설정 상관없이 Expose 로그에는 insert구문이 여러개 생김
            //실제 작동은 batch insert로 처리 됨.
            //https://github.com/JetBrains/Exposed/wiki/DSL#batch-insert
            pf.batchInsert(filesList) {
                this[pf.postId] = insertedPost[p.id] //외래키값 넣어줌
                this[pf.contentType] = it["contentType"] as String
                this[pf.originalFileName] = it["originalFileName"] as String
                this[pf.uuidFileName] = it["uuidFileName"] as String
            }

            //insert한 파일 목록을 post id로 조회
            val insertedPostFiles = pf.select { pf.postId eq insertedPost[p.id] }.map {r ->
                PostFileResponse(
                    id = r[pf.id].value,
                    postId = insertedPost[p.id],
                    uuidFileName = r[pf.uuidFileName],
                    originalFileName = r[pf.originalFileName],
                    contentType = r[pf.contentType]
                )
            }
            return@transaction PostWithFileResponse(
                id = insertedPost[p.id],
                title = insertedPost[p.title],
                content = insertedPost[p.content],
                createdDate = insertedPost[p.createdDate].toString(),
                files = insertedPostFiles
            )
        }

        //응답
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/paging/search/with-files")
    fun searchPagingWithFile(@RequestParam size : Int, @RequestParam page : Int, @RequestParam keyword : String?) : Page<PostWithFileResponse>
            = transaction(Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true) {
        // 검색 조건 생성
        val query = when {
            keyword != null -> Posts.select {
                (Posts.title like "%${keyword}%") or
                        (Posts.content like "%${keyword}%" ) }
            else -> Posts.selectAll()
        }
        // 전체 결과 카운트
        val totalCount = query.count()
        // 페이징 조회
        val postsResponse = query
            .orderBy(Posts.id to SortOrder.DESC)
            .limit(size, offset= (size * page).toLong())
            .map { r ->
                PostResponse(r[Posts.id],
                    r[Posts.title],
                    r[Posts.content], r[Posts.createdDate].toString())
            }
        val pf = PostFiles;
        // select * from post_file where post_id in (1,2,4,5,6.....)
        //inList 함수 (sql in 이 객체 안에 객체 배열을 넣어서 만들어줌. 중요함!!)
        val postFilesResponse = pf.select { pf.postId inList postsResponse.map { it.id }.toList() }.map {r ->
            PostFileResponse(
                id = r[pf.id].value,
                postId = r[pf.postId],
                uuidFileName = r[pf.uuidFileName],
                originalFileName = r[pf.originalFileName],
                contentType = r[pf.contentType]
            )
        }
        //파일 목록까지 포함된 리스트로 변환
        val postWithFileResponse = postsResponse.map { p ->
            PostWithFileResponse(
                id=p.id,
                title=p.title,
                content=p.content,
                createdDate = p.createdDate,
                // 각 포스트별 id가 매칭 되는 파일만 필터해서 추림
                files=postFilesResponse.filter { f -> f.postId == p.id }
            )
        }
        // Page 객체로 리턴
        PageImpl(postWithFileResponse, PageRequest.of(page, size),  totalCount)
    }

    //GET/posts/files/
    //Content-type: video/mp4
    @GetMapping("/files/{uuidFilename}")
    fun downloadFile(@PathVariable uuidFilename : String) : ResponseEntity<Any> {
        //서버에서 해당 경로의 파일을 읽어오기
        val file = Paths.get("$POST_FILE_PATH/$uuidFilename").toFile()
        //파일이 없으면 NOT-FOUND
        if (!file.exists()) {
            return ResponseEntity.notFound().build()
        }
        //파일의 content-type 처리
        val mimeType = Files.probeContentType(file.toPath())
        val mediaType = MediaType.parseMediaType(mimeType)

        //file: files/post/dkdkd.png
        val resource = resourceLoader.getResource("file:$file")
        return ResponseEntity.ok()
            .contentType(mediaType) //video/mp4, image/png, image/jpeg
            .body(resource)
    }


}