package com.eduzone.course;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository repo;

    public CourseService(CourseRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return repo.findAll();
    }

    @Transactional
    public Course create(CreateCourseRequest req) {
        if (repo.existsByCode(req.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã khoá học " + req.code() + " đã tồn tại");
        }
        Course course = repo.save(new Course(req.code(), req.title(), req.fee(), req.seatsAvailable()));
        log.info("Tạo khoá học {} với {} suất", course.getCode(), course.getSeatsAvailable());
        return course;
    }

    /** Bước 2 của luồng ghi danh: kiểm tra và giữ 1 suất. */
    @Transactional
    public Course reserveSeat(String code) {
        if (repo.reserveSeat(code) == 0) {
            if (!repo.existsByCode(code)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoá học " + code);
            }
            log.info("Khoá học {} đã hết suất ghi danh", code);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khoá học " + code + " đã hết suất ghi danh");
        }
        Course course = repo.findByCode(code).orElseThrow();
        log.info("Đã giữ 1 suất khoá học {}, còn lại {} suất", code, course.getSeatsAvailable());
        return course;
    }

    /** Bù trừ: trả lại 1 suất khi ghi danh bị huỷ. */
    @Transactional
    public Course releaseSeat(String code) {
        if (repo.releaseSeat(code) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoá học " + code);
        }
        Course course = repo.findByCode(code).orElseThrow();
        log.info("Đã trả lại 1 suất khoá học {}, còn lại {} suất", code, course.getSeatsAvailable());
        return course;
    }
}
