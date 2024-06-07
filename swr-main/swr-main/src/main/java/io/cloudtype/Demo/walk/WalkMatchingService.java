package io.cloudtype.Demo.walk;

import io.cloudtype.Demo.chat.ChatRoomService;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.mypage.pet.PetEntity;
import io.cloudtype.Demo.mypage.pet.PetRepository;
import io.cloudtype.Demo.mypage.user.PartnerRepository;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import io.cloudtype.Demo.notification.NotificationService;
import io.cloudtype.Demo.walk.DTO.WaiterDTO;
import io.cloudtype.Demo.walk.DTO.WalkMatchingDTO;
import io.cloudtype.Demo.walk.DTO.WalkMatchingDetailsDTO;
import io.cloudtype.Demo.walk.entity.WaiterEntity;
import io.cloudtype.Demo.walk.entity.WalkMatchingEntity;
import io.cloudtype.Demo.walk.entity.WalkRecordEntity;
import io.cloudtype.Demo.walk.repository.WaiterRepository;
import io.cloudtype.Demo.walk.repository.WalkMatchingRepository;
import io.cloudtype.Demo.walk.repository.WalkRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WalkMatchingService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final JWTUtil jwtUtil;
    private final WalkMatchingRepository walkMatchingRepository;
    private final WaiterRepository waiterRepository;
    private final WalkRecordRepository walkRecordRepository;
    private final ChatRoomService chatRoomService;
    private final PartnerRepository partnerRepository;
    private final NotificationService notificationService;

    @Autowired
    public WalkMatchingService(UserRepository userRepository, PetRepository petRepository, JWTUtil jwtUtil,
                               WalkMatchingRepository walkMatchingRepository,
                               WaiterRepository waiterRepository,
                               WalkRecordRepository walkRecordRepository,
                                 ChatRoomService chatRoomService,
                                 PartnerRepository partnerRepository,
                                    NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.jwtUtil = jwtUtil;
        this.walkMatchingRepository = walkMatchingRepository;
        this.waiterRepository = waiterRepository;
        this.walkRecordRepository = walkRecordRepository;
        this.chatRoomService = chatRoomService;
        this.partnerRepository = partnerRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void createPost(String accessToken, WalkMatchingEntity walkMatchingEntity, int isEdit) {
        UserEntity user = getUserEntity(accessToken);
        int amount;
        if (walkMatchingEntity.getWalkTime()==30) amount=15000;
        else if (walkMatchingEntity.getWalkTime()==60) amount=25000;
        else if (walkMatchingEntity.getWalkTime()==90) amount=30000;
        else if (walkMatchingEntity.getWalkTime()==120) amount=35000;
        else throw new IllegalArgumentException("산책 시간이 잘못되었습니다");
        if(user.getCoin()< amount) {
            throw new IllegalArgumentException("코인이 부족합니다 (산책비용 부족)");
        }

        int petId = walkMatchingEntity.getPetId();
        PetEntity pet = petRepository.findById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("해당 펫을 찾을 수 없습니다");
        }
        if (pet.getOwner().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 반려동물의 주인이 아닙니다");
        }
        //etc를 제외한 값이  모두 들어가야함
        if (pet.getOwner().getId() == 0 || pet.getPetName() == null || pet.getPetImage()== null || pet.getBirthYear()==0 || pet.getWeight() == 0
            || pet.getSpecies()==null || pet.getAnimalHospital()==null || pet.getVaccination()==null) {
            throw new IllegalArgumentException("필수 펫 정보가 누락 되어 신청글 작성이 불가능 합니다");
        }
        //산책글은 1개만 작성가능
        if (walkMatchingRepository.findByUser_Id(user.getId())!=null && isEdit == 0) {
            throw new IllegalArgumentException("이미 작성한 산책글이 있습니다");
        }

        WalkMatchingEntity post = new WalkMatchingEntity();
        if(isEdit == 1) {
            if(!waiterRepository.findByWalkMatchingId(walkMatchingEntity.getId()).isEmpty()) {
                throw new IllegalArgumentException("이미 신청자가 있는 게시글은 수정할 수 없습니다");
            }
            int id = walkMatchingEntity.getId();
            post = walkMatchingRepository.findById(id);
            if(post == null) {
                throw new IllegalArgumentException("해당 게시글을 찾을 수 없습니다");
            }
            if(post.getUser().getId()!=user.getId()) {
                throw new IllegalArgumentException("해당 게시글의 작성자가 아닙니다");
            }

            post.setCreateDate(LocalDateTime.now());
        }
        post.setUser(user);
        post.setPet(pet);
        post.setWalkTime(walkMatchingEntity.getWalkTime());
        post.setLatitude(walkMatchingEntity.getLatitude());
        post.setLongitude(walkMatchingEntity.getLongitude());
        post.setAddress(walkMatchingEntity.getAddress());
        post.setDetailAddress(walkMatchingEntity.getDetailAddress());
        post.setTitle(walkMatchingEntity.getTitle());
        post.setContent(walkMatchingEntity.getContent());
        post.setAmount(amount);

        walkMatchingRepository.save(post);
        //저장되면 DB 트리거에 의해서 자동 5분이 지나면 삭제, 단 status가 0인 경우에만 해당. 1로 바뛰면 유지
    }
    @Transactional
    public Page<WalkMatchingDTO> getList(String accessToken, int pageNumber, double nowLatitude, double nowLongitude, double maxDistance) {
        UserEntity user = getUserEntity(accessToken);
        if (user.getPartnership() == 0) {
            throw new IllegalArgumentException("파트너쉽이 없습니다");
        }

        List<WalkMatchingEntity> allMatchingByStatus = walkMatchingRepository.findByStatus(0);
        // 조회된 게시글들을 DTO로 변환하고, 조건에 맞는 게시글만 필터링 후 거리에 따라 정렬
        List<WalkMatchingDTO> filteredSorted = allMatchingByStatus.stream()
                .map(entity -> createDTO(entity, nowLatitude, nowLongitude)) // Entity를 DTO로 변환
                .filter(dto -> dto.getDistance() <= maxDistance) // 최대 거리 이내의 게시글만 필터링
                .sorted(Comparator.comparing(WalkMatchingDTO::getDistance)) // 거리에 따라 오름차순 정렬
                .collect(Collectors.toList()); // 스트림을 리스트로 변환

        // 페이지 처리를 위해 PageRequest 객체 생성
        Pageable pageable = PageRequest.of(pageNumber, 10);
        // 페이지 시작 인덱스 계산
        int start = (int) pageable.getOffset();
        // 페이지 종료 인덱스 계산
        int end = Math.min((start + pageable.getPageSize()), filteredSorted.size());

        if (start >= end) {
            log.info("Requested page is out of filtered data range.");
            throw new IllegalArgumentException("요청한 페이지가 존재하지 않습니다 (페이지 범위 초과) 혹은 글이 없을 수 있습니다");
        }
        // 계산된 시작과 종료 인덱스를 사용하여 현재 페이지의 콘텐츠를 추출
        List<WalkMatchingDTO> pageContent = filteredSorted.subList(start, end);

        // PageImpl를 사용하여 페이지 정보와 함께 DTO 리스트를 페이지 객체로 반환
        return new PageImpl<>(pageContent, pageable, filteredSorted.size());
    }

    private WalkMatchingDTO createDTO(WalkMatchingEntity entity, double nowLatitude, double nowLongitude) {
        WalkMatchingDTO dto = new WalkMatchingDTO();
        dto.setId(entity.getId());
        dto.setUserNickname(entity.getUser().getNickname());
        dto.setPetId(entity.getPet().getId());
        dto.setWalkTime(entity.getWalkTime());
        dto.setCreateDate(entity.getCreateDate());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setAddress(entity.getAddress());
        dto.setDetailAddress(entity.getDetailAddress());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        if(nowLongitude == 0 && nowLatitude == 0) {
            dto.setDistance(0);
            return dto;
        }
        double distance = getDistance(entity.getLatitude(), entity.getLongitude(), nowLatitude, nowLongitude);
        dto.setDistance(distance); // 거리 설정
        return dto;
    }

    private double getDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구의 반경 (킬로미터 단위)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        // BigDecimal을 사용하여 소수점 3 번째 자리에서 반올림
        BigDecimal bd = new BigDecimal(distance);
        bd = bd.setScale(3, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    @Transactional
    public void apply(String accessToken, int postId) {
        UserEntity user = getUserEntity(accessToken);
        if (user.getPartnership() == 0) {
            throw new IllegalArgumentException("파트너쉽이 없습니다");
        }
        WalkMatchingEntity post = walkMatchingRepository.findById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글을 찾을 수 없습니다");
        }
        if (post.getUser().getId() == user.getId()) {
            throw new IllegalArgumentException("본인의 게시글에는 신청할 수 없습니다");
        }
        if (walkMatchingRepository.countByWalker_IdAndStatus(user.getId(),1)>0) {
            throw new IllegalArgumentException("이미 산책중 입니다");
        }
        if (waiterRepository.findByWalkMatchingIdAndWaiterId(postId, user.getId())!=null) {
            throw new IllegalArgumentException("이미 신청한 게시글입니다");
        }
        WaiterEntity waiter = new WaiterEntity();
        waiter.setWalkMatching(post);
        waiter.setWriter(post.getUser());
        waiter.setWaiter(user);
        waiterRepository.save(waiter);

        //신청이 완료되었음으로 주인에게 신청 알람을 보내야함
        if (notificationService.isUserConnected(user.getUsername())) {
            notificationService.notifyUser(user.getUsername(), "신청을 하였습니다");
        } else {
            log.warn("Partner " + user.getUsername() + " is not connected. Notification not sent.");
        }
        UserEntity customer = post.getUser();
        if (notificationService.isUserConnected(customer.getUsername())) {
            notificationService.notifyUser(customer.getUsername(), "산책글에 신청이 왔습니다");
        }else {
            log.warn("consumer " + customer.getUsername() + " is not connected. Notification not sent.");
        }
    }
    public WalkMatchingDetailsDTO myPost(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        WalkMatchingEntity post = walkMatchingRepository.findByUser_Id(user.getId());
        if (post == null) {
            throw new IllegalArgumentException("작성한 게시글이 없습니다");
        }
        return createWalkDTO(post);
    }
    private WalkMatchingDetailsDTO createWalkDTO(WalkMatchingEntity entity) {
        WalkMatchingDetailsDTO dto = new WalkMatchingDetailsDTO();
        dto.setId(entity.getId());
        dto.setUserNickname(entity.getUser().getNickname());
        dto.setUserProfile(entity.getUser().getProfileImage());
        dto.setPetId(entity.getPet().getId());
        dto.setWalkTime(entity.getWalkTime());
        dto.setCreateDate(entity.getCreateDate());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setStatus(entity.getStatus());
        dto.setAddress(entity.getAddress());
        dto.setDetailAddress(entity.getDetailAddress());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        if( entity.getStartTime() != null) {
            dto.setStartTime(entity.getStartTime());
        } else {
            dto.setStartTime(null);
        }
        if (entity.getWalker() != null) {
            dto.setWalkerNickname(entity.getWalker().getNickname());
            dto.setWalkerProfile(partnerRepository.findByUser_Id(entity.getWalker().getId()).getPartnerProfileImage());
        } else {
            dto.setWalkerNickname(null);
            dto.setWalkerProfile(null);
        }
        return dto;
    }
    public List<WaiterDTO> getApplierList(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        WalkMatchingEntity post = walkMatchingRepository.findByUser_Id(user.getId());

        List<WaiterEntity> waiters = waiterRepository.findByWalkMatchingId(post.getId());

        return waiters.stream()
                .map(waiter -> createDTO3(waiter.getWaiter(),waiter.getId()))
                .sorted(Comparator.comparing(WaiterDTO::getRating).reversed())
                .collect(Collectors.toList());
    }
    private WaiterDTO createDTO3(UserEntity user, int id) {
        WaiterDTO dto = new WaiterDTO();
        dto.setId(id);
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setProfileImage(partnerRepository.findByUser_Id(user.getId()).getPartnerProfileImage());
        dto.setRating(user.getWalkRating());
        dto.setReviewCount(user.getWalkReviewCount());
        return dto;
    }
    public List<WalkMatchingDTO> myApply(String accessToken,double nowLatitude, double nowLongitude) {
        UserEntity user = getUserEntity(accessToken);
        if(user.getPartnership()==0) {
            throw new IllegalArgumentException("파트너쉽이 없습니다");
        }

        List<WaiterEntity> waiters = waiterRepository.findByWaiterId(user.getId());
        if (waiters.isEmpty()) {
            throw new IllegalArgumentException("신청한 게시글이 없습니다");
        }
        return waiters.stream()
                .map(waiter -> createDTO2(waiter.getWalkMatching(), nowLatitude, nowLongitude))
                .sorted(Comparator.comparing(WalkMatchingDTO::getDistance))
                .collect(Collectors.toList());

    }
    private WalkMatchingDTO createDTO2(WalkMatchingEntity walkMatching, double nowLatitude, double nowLongitude) {
        WalkMatchingDTO dto = new WalkMatchingDTO();
        dto.setId(walkMatching.getId());
        dto.setUserNickname(walkMatching.getUser().getNickname());
        dto.setPetId(petRepository.findById(walkMatching.getPet().getId()).getId());    //이게 맞나?
        dto.setWalkTime(walkMatching.getWalkTime());
        dto.setCreateDate(walkMatching.getCreateDate());
        dto.setAddress(walkMatching.getAddress());
        dto.setDetailAddress(walkMatching.getDetailAddress());
        dto.setTitle(walkMatching.getTitle());
        dto.setContent(walkMatching.getContent());
        double distance = getDistance(walkMatching.getLatitude(), walkMatching.getLongitude(), nowLatitude, nowLongitude);
        dto.setDistance(distance); // 거리 설정
        return dto;
    }
    public WalkMatchingDetailsDTO myApplyProgress(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        WalkMatchingEntity post = walkMatchingRepository.findByWalker_Id(user.getId());
        if (post == null) {
            throw new IllegalArgumentException("진행중인 산책이 없습니다");
        }
        if(post.getStatus()==0) {
            throw new IllegalArgumentException("아직 수락되지 않은 게시글입니다");
        }
        return createWalkDTO(post);
    }
    @Transactional
    public void accept(String accessToken, int waiterListId, int postId, int waiterId) {
        UserEntity user = getUserEntity(accessToken);
        //교차검증
        WaiterEntity waiter = waiterRepository.findById(waiterListId);
        if(waiter == null || waiter.getId()!=waiterListId || waiter.getWalkMatching().getId()!=postId
                || waiter.getWaiter().getId()!=waiterId) {
            throw new IllegalArgumentException("신청 정보가 없습니다");
        }
        WalkMatchingEntity post = walkMatchingRepository.findById(postId);
        post.setWalker(waiter.getWaiter());
        post.setStatus(1);
        walkMatchingRepository.save(post);
        //해당글의 신청자들 삭제
        waiterRepository.deleteByWalkMatchingId(postId);
        //매칭이 된 신청자의 다른 신청들삭제
        waiterRepository.deleteByWaiterId(waiterId);

        UserEntity partner = post.getWalker();
        // 채팅방 생성(name : username+walkername, username : username, walkername : )
        chatRoomService.createChatRoom(user, partner);

        // 매칭 완료 시, 두 사용자에게 알림을 보냄
        if (notificationService.isUserConnected(user.getUsername())) {
            notificationService.notifyUser(user.getUsername(), "매칭이 성사되었습니다");
        } else {
            log.warn("User " + user.getUsername() + " is not connected. Notification not sent.");
        }
        if (notificationService.isUserConnected(partner.getUsername())) {
            notificationService.notifyUser(partner.getUsername(), "매칭이 성사되었습니다");
        }else {
            log.warn("Partner " + partner.getUsername() + " is not connected. Notification not sent.");
        }
    }
    @Transactional
    public void deletePost(String accessToken, int postId) {
        UserEntity user = getUserEntity(accessToken);
        WalkMatchingEntity post = walkMatchingRepository.findById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글을 찾을 수 없습니다");
        }
        if (post.getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 게시글의 작성자가 아닙니다");
        }
        if(post.getStatus()==1 || post.getStatus()==2) {
            throw new IllegalArgumentException("이미 수락된 게시글입니다");
        }
        walkMatchingRepository.delete(post);
        waiterRepository.deleteByWalkMatchingId(postId);
    }
    @Transactional
    public void deleteApply(String accessToken, int postId) {
        UserEntity user = getUserEntity(accessToken);
        WalkMatchingEntity post = walkMatchingRepository.findById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글을 찾을 수 없습니다");
        }
        if (post.getUser().getId() == user.getId()) {
            throw new IllegalArgumentException("본인의 게시글에는 신청할 수 없습니다");
        }
        WaiterEntity waiter = waiterRepository.findByWalkMatchingIdAndWaiterId(postId, user.getId());
        if (waiter == null) {
            throw new IllegalArgumentException("신청한 게시글이 없습니다");
        }
        waiterRepository.delete(waiter);
    }
    @Transactional
    public void start(String accessToken, int postId) {
        UserEntity user = getUserEntity(accessToken);
        WalkMatchingEntity post = walkMatchingRepository.findById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글을 찾을 수 없습니다");
        }
        if (post.getWalker().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 게시글의 파트너가 아닙니다");
        }
        if (post.getStatus() != 1) {
            throw new IllegalArgumentException("매칭된 게시글이 아닙니다");
        }
        post.setStatus(2);
        post.setStartTime(LocalDateTime.now());
        walkMatchingRepository.save(post);

        // 산책 시작시 알림
        if (notificationService.isUserConnected(user.getUsername())) {
            notificationService.notifyUser(user.getUsername(), "산책이 시작되었습니다");
        } else {
            log.warn("Partner " + user.getUsername() + " is not connected. Notification not sent.");
        }
        UserEntity customer = post.getUser();
        if (notificationService.isUserConnected(customer.getUsername())) {
            notificationService.notifyUser(customer.getUsername(), "산책이 시작되었습니다");
        }else {
            log.warn("consumer " + customer.getUsername() + " is not connected. Notification not sent.");
        }
    }
    @Transactional
    public int end(String accessToken, int postId) {
        UserEntity user = getUserEntity(accessToken);
        WalkMatchingEntity post = walkMatchingRepository.findById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글을 찾을 수 없습니다");
        }
        if (post.getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 게시글의 작성자가 아닙니다");
        }
        if (post.getStatus() != 2) {
            throw new IllegalArgumentException("산책중인 게시글이 아닙니다");
        }
        if(post.getUser().getCoin()< post.getAmount()) {
            throw new IllegalArgumentException("코인이 부족합니다 충전을 해주세요");
        }
        //산책 시작 시간과 현재 시간을 빼서 실제 산책 시간을 계산하여, 처음 요구한 walkTime을 넘었는지를 비교
        int walkTime = post.getWalkTime();
        LocalDateTime startTime = post.getStartTime();  // 산책 시작 시간
        LocalDateTime now = LocalDateTime.now();  // 현재 시간
        long minutesBetween = Duration.between(startTime, now).toMinutes();  // 시작 시간과 현재 시간의 차이 (분)

        if(minutesBetween < walkTime) { // 계산된 시간이 주어진 walkTime보다 큰지 비교
            throw new IllegalArgumentException("산책 시간을 충족하지 못하였습니다");
        }
        //고객의 토큰 계산내용 들가야함.
        post.getUser().setCoin(post.getUser().getCoin()-post.getAmount());

        int status = 3; // 산책 완료
        String reason = "no problem"; // 문제가 없을 경우
        //레코드 테이블로 이동
        int recodeId = createRecord(post, now, status, reason);
        walkMatchingRepository.delete(post);

        // 산책 종료시 알림
        if (notificationService.isUserConnected(user.getUsername())) {
            notificationService.notifyUser(user.getUsername(), "산책이 종료되었습니다");
        } else {
            log.warn("User " + user.getUsername() + " is not connected. Notification not sent.");
        }
        UserEntity partner = post.getWalker();
        if (notificationService.isUserConnected(partner.getUsername())) {
            notificationService.notifyUser(partner.getUsername(), "산책이 종료되었습니다");
        }else {
            log.warn("Partner " + partner.getUsername() + " is not connected. Notification not sent.");
        }
        return recodeId;
    }
    private int createRecord(WalkMatchingEntity post, LocalDateTime now, int status, String reason) {
        WalkRecordEntity walkRecode = new WalkRecordEntity();
        walkRecode.setUser(post.getUser());
        walkRecode.setPet(post.getPet());
        walkRecode.setWalkTime(post.getWalkTime());
        walkRecode.setStartTime(post.getStartTime());
        walkRecode.setEndTime(now);
        walkRecode.setCreateDate(post.getCreateDate());
        walkRecode.setLatitude(post.getLatitude());
        walkRecode.setLongitude(post.getLongitude());
        walkRecode.setAddress(post.getAddress());
        walkRecode.setDetailAddress(post.getDetailAddress());
        walkRecode.setTitle(post.getTitle());
        walkRecode.setContent(post.getContent());
        walkRecode.setWalker(post.getWalker());
        if(status == 3) {
            walkRecode.setStatus(3);
        } else if(status == 4) {
            walkRecode.setStatus(4);
            walkRecode.setReason(reason);
        }
        walkRecode.setAmount(post.getAmount());
        walkRecordRepository.save(walkRecode);
        return walkRecode.getId();
    }
    @Transactional
    public void incomplete(String accessToken, int postId, String reason) {
        UserEntity user = getUserEntity(accessToken);
        WalkMatchingEntity post = walkMatchingRepository.findById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글을 찾을 수 없습니다");
        }
        if (post.getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 게시글의 작성자가 아닙니다");
        }
        if (post.getStatus() ==0) {
            throw new IllegalArgumentException("산책중인 게시글이 아닙니다");
        }
        int status = 4; // 산책 미완료
        //레코드 테이블로 이동
        int walkRecodeId = createRecord(post, LocalDateTime.now(), status, reason);
        walkMatchingRepository.delete(post);

        // 산책 비정상종료시 알림
        if (notificationService.isUserConnected(user.getUsername())) {
            notificationService.notifyUser(user.getUsername(), "문제가 발생하여 산책을 종료합니다");
        } else {
            log.warn("User " + user.getUsername() + " is not connected. Notification not sent.");
        }
        UserEntity partner = post.getWalker();
        if (notificationService.isUserConnected(partner.getUsername())) {
            notificationService.notifyUser(partner.getUsername(), "문제가 발생하여 산책을 종료합니다");
        }else {
            log.warn("Partner " + partner.getUsername() + " is not connected. Notification not sent.");
        }
    }
    public int getUserWalkStatus(String accessToken,int status) {
        UserEntity user = getUserEntity(accessToken);
        if(status==0) return walkMatchingRepository.countByUser_IdAndStatus(user.getId(),0);
        else if (status==1) return walkMatchingRepository.countByUser_IdAndStatus(user.getId(),1);
        else if (status==2) return walkMatchingRepository.countByUser_IdAndStatus(user.getId(),2);
        else return 0;
    }
    public int getWalkerStatus(String accessToken,int status) {
        UserEntity user = getUserEntity(accessToken);
        if (status==1) return walkMatchingRepository.countByWalker_IdAndStatus(user.getId(),1);
        else if (status==2) return walkMatchingRepository.countByWalker_IdAndStatus(user.getId(),2);
        else return 0;
    }
    private UserEntity getUserEntity(String accessToken){
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        return user;
    }
}