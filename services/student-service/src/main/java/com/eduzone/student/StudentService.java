package com.eduzone.student;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository repo;

    public StudentService(StudentRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Student get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy học viên " + id));
    }

    @Transactional
    public Student create(CreateStudentRequest req) {
        if (repo.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email " + req.email() + " đã được đăng ký");
        }
        Student student = repo.save(new Student(req.fullName(), req.email(), req.walletBalance()));
        log.info("Tạo học viên {} với số dư ví {} VND", student.getId(), student.getWalletBalance());
        return student;
    }

    /** Bước 3 của luồng ghi danh: trừ học phí trong ví. */
    @Transactional
    public Student debit(Long id, WalletRequest req) {
        if (repo.debit(id, req.amount()) == 0) {
            Student student = get(id); // ném 404 nếu không có học viên
            log.info("Học viên {} không đủ số dư: có {} VND, cần {} VND (ghi danh {})",
                    id, student.getWalletBalance(), req.amount(), req.enrollmentId());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Số dư ví không đủ để thanh toán " + req.amount() + " VND");
        }
        Student student = get(id);
        log.info("Đã trừ {} VND ví học viên {} cho ghi danh {}, số dư còn {} VND",
                req.amount(), id, req.enrollmentId(), student.getWalletBalance());
        return student;
    }

    /** Bước 5 (bù trừ): hoàn học phí khi ghi danh bị huỷ. */
    @Transactional
    public Student refund(Long id, WalletRequest req) {
        if (repo.credit(id, req.amount()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy học viên " + id);
        }
        Student student = get(id);
        log.info("Đã hoàn {} VND vào ví học viên {} cho ghi danh {}, số dư còn {} VND",
                req.amount(), id, req.enrollmentId(), student.getWalletBalance());
        return student;
    }
}
