package io.cloudtype.Demo.mail;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class MailSendService {

    private final JavaMailSender mailSender;
    private final EmailCheckRepository emailCheckRepository;
    @Autowired
    public MailSendService(JavaMailSender mailSender, EmailCheckRepository emailCheckRepository){
        this.mailSender=mailSender;
        this.emailCheckRepository=emailCheckRepository;
    }

    //임의의 6자리 양수를 반환합니다.
    public int makeRandomNumber() {
        Random r = new Random();
        String randomNumber = "";
        for(int i = 0; i < 6; i++) {
            randomNumber += Integer.toString(r.nextInt(10));
        }

        return Integer.parseInt(randomNumber);
    }



    //mail을 어디서 보내는지, 어디로 보내는지 , 인증 번호를 html 형식으로 어떻게 보내는지 작성합니다.
    public String joinEmail(String email) {
        int authNumber =  makeRandomNumber();
        String setFrom = "teamswr2019@gmail.com"; // email-config에 설정한 자신의 이메일 주소를 입력
        String toMail = email;
        String title = "회원 가입 인증 이메일 입니다."; // 이메일 제목
        String content =
                "SWR APP을 방문해주셔서 감사합니다." + 	//html 형식으로 작성 !
                        "<br><br>" +
                        "인증 번호는 " + authNumber + "입니다." +
                        "<br>" +
                        "인증번호를 제대로 입력해주세요"; //이메일 내용 삽입
        mailSend(setFrom, toMail, title, content);
        return Integer.toString(authNumber);
    }
    @Transactional
    public void saveAuthNum(String email,String authNum){
        //만일 해당 이메일이 이미 있다면 6자리를 새로 갱신
        //없다면 새로 생성
        EmailCheckEntity emailCheckEntity=emailCheckRepository.findByEmail(email);
        if(emailCheckEntity==null){
            emailCheckEntity=new EmailCheckEntity();
            emailCheckEntity.setEmail(email);
            emailCheckEntity.setAuthNum(authNum);
            emailCheckRepository.save(emailCheckEntity);
        }
        else{
            emailCheckEntity.setAuthNum(String.valueOf(authNum));
            emailCheckEntity.setTryDateTime(LocalDateTime.now());
            emailCheckRepository.save(emailCheckEntity);
        }
    }

    //이메일을 전송합니다.
    public void mailSend(String setFrom, String toMail, String title, String content) {
        MimeMessage message = mailSender.createMimeMessage();//JavaMailSender 객체를 사용하여 MimeMessage 객체를 생성
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message,true,"utf-8");//이메일 메시지와 관련된 설정을 수행합니다.
            // true를 전달하여 multipart 형식의 메시지를 지원하고, "utf-8"을 전달하여 문자 인코딩을 설정
            helper.setFrom(setFrom);//이메일의 발신자 주소 설정
            helper.setTo(toMail);//이메일의 수신자 주소 설정
            helper.setSubject(title);//이메일의 제목을 설정
            helper.setText(content,true);//이메일의 내용 설정 두 번째 매개 변수에 true를 설정하여 html 설정으로한다.
            mailSender.send(message);
        } catch (MessagingException e) {//이메일 서버에 연결할 수 없거나, 잘못된 이메일 주소를 사용하거나, 인증 오류가 발생하는 등 오류
            // 이러한 경우 MessagingException이 발생
            e.printStackTrace();//e.printStackTrace()는 예외를 기본 오류 스트림에 출력하는 메서드
        }
    }

    //인증번호가 맞는지 확인합니다.
    @Transactional
    public Boolean checkAuthNum(String email,String authNum){
        EmailCheckEntity emailCheckEntity=emailCheckRepository.findByEmail(email);
        if(emailCheckEntity==null){
            throw new IllegalArgumentException("해당 이메일 요청내역이 없습니다");
        }
        //인증 요청 시간이 5분이 지났다면 인증번호를 삭제하고 예외를 던집니다. getTryDateTime은 인증시작시간.
        if(emailCheckEntity.getTryDateTime().plusMinutes(5).isBefore(LocalDateTime.now())){
            emailCheckRepository.delete(emailCheckEntity);
            throw new IllegalArgumentException("인증 시간이 초과되었습니다.");
        }
        if(emailCheckEntity.getAuthNum().equals(authNum)){
            emailCheckRepository.delete(emailCheckEntity);
            return true;
        }
        else{
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }
        //db에 24시간 마다 체크해서 인증 요청이 5분이 지났다면 삭제하는 이벤트 넣어야함.
    }
}
