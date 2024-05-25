package io.cloudtype.Demo.service;

import io.cloudtype.Demo.Dto.Care.CareApplyDTO;
import io.cloudtype.Demo.Dto.Care.CareApplyDetailDTO;
import io.cloudtype.Demo.Dto.Care.CarePostDetailsDTO;
import io.cloudtype.Demo.Dto.Care.CarePostListDTO;
import io.cloudtype.Demo.entity.Care.*;
import io.cloudtype.Demo.entity.PetEntity;
import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.repository.Care.*;
import io.cloudtype.Demo.repository.PetRepository;
import io.cloudtype.Demo.repository.UserRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CareMatchingService {

    private final UserRepository userRepository;
    private final CarePostRepository carePostRepository;
    private final CareImgRepository careImgRepository;
    private final JWTUtil jwtUtil;
    private final ReservationSchedulerRepository reservationSchedulerRepository;
    private final PetRepository petRepository;
    private final CareMatchingRepository careMatchingRepository;
    private final CommunityBoardService communityBoardService;
    private final CareRecordRepository careRecordRepository;
    private final SseService sseService;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public CareMatchingService(UserRepository userRepository, JWTUtil jwtUtil,
            CarePostRepository carePostRepository,
            CareImgRepository careImgRepository,
            ReservationSchedulerRepository reservationSchedulerRepository,
            PetRepository petRepository,
            CareMatchingRepository careMatchingRepository,
            CommunityBoardService communityBoardService,
            CareRecordRepository careRecordRepository,
            SseService sseService
    ) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.carePostRepository = carePostRepository;
        this.careImgRepository = careImgRepository;
        this.petRepository = petRepository;
        this.careMatchingRepository = careMatchingRepository;
        this.communityBoardService = communityBoardService;
        this.reservationSchedulerRepository = reservationSchedulerRepository;
        this.careRecordRepository = careRecordRepository;
        this.sseService = sseService;
    }

    @Transactional
    public int createPost(String accessToken, String title, String content,
                           String administrativeAddress1, String administrativeAddress2, String streetNameAddress, String detailAddress) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        //파트너쉽이 있는지 확인
        if (user.getPartnership() != 1) {
            throw new IllegalStateException("파트너쉽이 없습니다");
        }
        //이미 작성한 글이 있는지 확인
        if (carePostRepository.findByCaregiver_Id(user.getId()) != null) {
            throw new IllegalStateException("이미 작성한 글이 있습니다");
        }
        //글 작성
        CarePostEntity carePost = new CarePostEntity();
        carePost.setCaregiver(user);
        carePost.setTitle(title);
        carePost.setContent(content);
        carePost.setAdministrativeAddress1(administrativeAddress1);
        carePost.setAdministrativeAddress2(administrativeAddress2);
        carePost.setStreetNameAddress(streetNameAddress);
        carePost.setDetailAddress(detailAddress);
        carePostRepository.save(carePost);
        return carePost.getId();
    }
    @Transactional
    public void addImage(int carePostId, String imageUrl, String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        //carePostId로 CarePostEntity 찾고 이미지 저장
        CarePostEntity carePost = carePostRepository.findById(carePostId);
        CareImgEntity careImg = new CareImgEntity();
        careImg.setCarePost(carePost);
        careImg.setImgUrl(imageUrl);
        careImg.setUploadDate(LocalDateTime.now());
        careImgRepository.save(careImg);
    }
    //돌봄글 첫 작성시
    @Transactional
    public void addUnavailableDates(int carePostId, List<String> unavailableDates) {
        CarePostEntity carePost = carePostRepository.findById(carePostId);

        List<Date> dates = unavailableDates.stream().map(dateStr -> {
            try {
                LocalDate localDate = LocalDate.parse(dateStr, dateFormatter);
                return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Invalid date format", e);
            }
        }).toList();

        for (Date unavailableDate : dates) {
            ReservationSchedulerEntity unavailableDateEntity = new ReservationSchedulerEntity();
            unavailableDateEntity.setCarePost(carePost);
            unavailableDateEntity.setUnavailableDate(unavailableDate);
            reservationSchedulerRepository.save(unavailableDateEntity);
        }
    }

    @Transactional
    public void applyCare(String accessToken, CareApplyDTO careApplyDTO) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        CarePostEntity carePost = carePostRepository.findById(careApplyDTO.getCarePostId());
        if (carePost == null) {
            throw new IllegalArgumentException("돌봄글을 찾을 수 없습니다");
        }
        if (carePost.getCaregiver().getId() == user.getId()) {
            throw new IllegalStateException("자신의 글에는 신청할 수 없습니다");
        }
        if(careApplyDTO.getAmount()> user.getCoin()){
            throw new IllegalStateException("코인이 부족합니다 충전후 이용해주세요");
        }

        // 예약 시작일과 종료일을 LocalDate로 변환하여 날짜 리스트를 만듭니다.
        LocalDate startDate = careApplyDTO.getReservationStartDate().toLocalDate();
        LocalDate endDate = careApplyDTO.getReservationEndDate().toLocalDate();
        List<LocalDate> reservationDates = startDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toList());
        log.info("startDate: {}", startDate);
        log.info("endDate: {}", endDate);
        log.info("reservationDates: {}", reservationDates);

        // 예약 불가 날짜와 겹치는지 확인
        List<ReservationSchedulerEntity> reservationSchedulers = reservationSchedulerRepository.findByCarePost(carePost);
        for (LocalDate date : reservationDates) {
            boolean isUnavailable = reservationSchedulers.stream()
                    .anyMatch(reservation -> reservation.getUnavailableDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate().isEqual(date));
            if (isUnavailable) {
                throw new IllegalStateException("예약 기간에 예약 불가 날짜가 포함되어 있습니다.");
            }
        }

        //예약 날짜가 현재 날짜보다 이후여야함
        if (careApplyDTO.getReservationEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("예약 날짜가 현재 날짜보다 이전입니다");
        }
        PetEntity pet = petRepository.findById(careApplyDTO.getPetId());
        if(pet == null) {
            throw new IllegalArgumentException("펫을 찾을 수 없습니다");
        }
        int dtoPetOwnerId = pet.getOwner().getId();
        if(dtoPetOwnerId != user.getId()) {
            throw new IllegalArgumentException("자신의 펫이 아닙니다");
        }
        //신청한 날짜와 시간이 현재 시간+3시간보다 이후인지를 확인하고해야함.
        if (careApplyDTO.getReservationEndDate().isBefore(LocalDateTime.now().plusHours(3))) {
            throw new IllegalStateException("3시간 전에만 예약할 수 있습니다");
        }
        CareMatchingEntity careMatching = new CareMatchingEntity();
        careMatching.setCarePost(carePost);
        careMatching.setPet(pet);
        careMatching.setOwner(user);
        careMatching.setAmount(careApplyDTO.getAmount());
        careMatching.setApplyDate(LocalDateTime.now());
        careMatching.setReservationStartDate(careApplyDTO.getReservationStartDate());
        careMatching.setReservationEndDate(careApplyDTO.getReservationEndDate());
        careMatching.setRequestMessage(careApplyDTO.getRequestMessage());
        careMatchingRepository.save(careMatching);

        //돌봄글 작성자에게 신청자가 있음을 알림
        sseService.notifyMatch(String.valueOf(carePost.getCaregiver().getId()), "신청자가 있습니다", 2);
    }

    @Transactional
    public void deleteImages(int carePostId, String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        CarePostEntity carePost = carePostRepository.findById(carePostId);
        if (carePost == null) {
            throw new IllegalArgumentException("돌봄글을 찾을 수 없습니다");
        }
        if (carePost.getCaregiver().getId() != user.getId()) {
            throw new IllegalStateException("자신의 글만 수정할 수 있습니다");
        }
        List<CareImgEntity> careImages = careImgRepository.findByCarePost_Id(carePostId);
        for (CareImgEntity careImage : careImages) {
            communityBoardService.deleteImageGcs(careImage.getImgUrl());
            careImgRepository.delete(careImage);
        }
    }
    @Transactional
    public void updateUnavailableDates(int carePostId, List<String> newUnavailableDates) {
        // 새로운 unavailableDates를 LocalDate 타입으로 변환
        List<LocalDate> newDates = newUnavailableDates.stream()
                .map(dateStr -> LocalDate.parse(dateStr, dateFormatter))
                .collect(Collectors.toList());

        // 기존 unavailableDates를 조회
        CarePostEntity carePost = carePostRepository.findById(carePostId);

        List<ReservationSchedulerEntity> existingReservations = reservationSchedulerRepository.findByCarePost(carePost);

        Set<LocalDate> existingDates = existingReservations.stream()
                .map(reservation -> reservation.getUnavailableDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toSet());

        Set<LocalDate> newDatesSet = new HashSet<>(newDates);

        // 추가해야 할 날짜 (newDatesSet - existingDates)
        Set<LocalDate> datesToAdd = newDatesSet.stream()
                .filter(date -> !existingDates.contains(date))
                .collect(Collectors.toSet());

        // 삭제해야 할 날짜 (existingDates - newDatesSet)
        Set<LocalDate> datesToRemove = existingDates.stream()
                .filter(date -> !newDatesSet.contains(date))
                .collect(Collectors.toSet());

        // 이용불가 날짜 추가
        for (LocalDate localDate : datesToAdd) {
            Date dateToAdd = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            boolean isExist = reservationSchedulerRepository.existsByCarePostAndReservationDate(carePost, dateToAdd);
            if (isExist) {
                throw new IllegalArgumentException("해당 날짜에 이미 예약이 있어서 이용불가날짜로의 변경이 불가능합니다");
            } else {
                ReservationSchedulerEntity reservation = new ReservationSchedulerEntity();
                reservation.setCarePost(carePost);
                reservation.setUnavailableDate(dateToAdd);
                reservationSchedulerRepository.save(reservation);
            }
        }

        // 이용불가 날짜 -> 이용가능 날짜 변경 (이용불가날짜 리스트에서 삭제)
        for (LocalDate localDate : datesToRemove) {
            Date dateToRemove = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            ReservationSchedulerEntity reservationEntity = reservationSchedulerRepository.findByCarePostAndUnavailableDate(carePost, dateToRemove);
            if (reservationEntity != null) {
                reservationSchedulerRepository.delete(reservationEntity);
            }
        }
    }
    @Transactional
    public void editPost(String accessToken, int carePostId, String title, String content) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        CarePostEntity carePost = carePostRepository.findById(carePostId);
        if (carePost == null) {
            throw new IllegalArgumentException("돌봄글을 찾을 수 없습니다");
        }
        if (carePost.getCaregiver().getId() != user.getId()) {
            throw new IllegalStateException("자신의 글만 수정할 수 있습니다");
        }
        carePost.setTitle(title);
        carePost.setContent(content);
        carePostRepository.save(carePost);
    }
    public boolean checkReservation(int carePostId) {
        return careMatchingRepository.existsByCarePost_IdAndStatus(carePostId,0);
    }
    @Transactional
    public void deletePost(String accessToken, int carePostId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        CarePostEntity carePost = carePostRepository.findById(carePostId);
        if (carePost == null) {
            throw new IllegalArgumentException("돌봄글을 찾을 수 없습니다");
        }
        if (carePost.getCaregiver().getId() != user.getId()) {
            throw new IllegalStateException("자신의 글만 삭제할 수 있습니다");
        }
        //돌봄예약신청이 없고, 예약수가 0이여야 삭제
        if (careMatchingRepository.existsByCarePost_IdAndStatus(carePostId,0)) {
            throw new IllegalStateException("예약신청이 있는 글은 삭제할 수 없습니다");
        }
        if(carePost.getReservations() != 0) {
            throw new IllegalStateException("예약이 잡힌 글은 삭제할 수 없습니다");
        }
        //돌봄글의 이미지 삭제
        List<CareImgEntity> careImages = careImgRepository.findByCarePost_Id(carePostId);
        for (CareImgEntity careImage : careImages) {
            communityBoardService.deleteImageGcs(careImage.getImgUrl());
            careImgRepository.delete(careImage);
        }
        carePostRepository.delete(carePost);
    }
    public Page<CarePostListDTO> list(String accessToken, int page, String administrativeAddress1, String administrativeAddress2, double homeLatitude, double homeLongitude) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        List<CarePostEntity> carePostList = carePostRepository.findByAdministrativeAddress1AndAdministrativeAddress2(administrativeAddress1, administrativeAddress2); //1차적으로 시도, 시군구로 필터링
        List<CarePostListDTO> filteredCarePostList = carePostList.stream()
                .map(entity -> entityToDto(entity,homeLatitude, homeLongitude))
                .sorted(Comparator.comparing(CarePostListDTO::getCarePostId))
                .toList();

        Pageable pageable = PageRequest.of(page, 10);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredCarePostList.size());
        if (start >= end) {
            log.info("Requested page is out of filtered data range.");
            throw new IllegalArgumentException("요청한 페이지가 존재하지 않습니다 (페이지 범위 초과) 혹은 글이 없을 수 있습니다");
        }
        List<CarePostListDTO> pageContent = filteredCarePostList.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filteredCarePostList.size());

    }
    private CarePostListDTO entityToDto(CarePostEntity entity, double homeLatitude, double homeLongitude) {
        double distance = getDistance(entity.getLatitude(), entity.getLongitude(), homeLatitude, homeLongitude);
        CarePostListDTO dto = new CarePostListDTO();
        dto.setCarePostId(entity.getId());
        dto.setCaregiverNickname(entity.getCaregiver().getNickname());
        dto.setCaregiverRating(entity.getCaregiver().getCareRating());
        dto.setCaregiverReviewCount(entity.getCaregiver().getCareReviewCount());
        dto.setTitle(entity.getTitle());
        dto.setAdministrativeAddress1(entity.getAdministrativeAddress1());
        dto.setAdministrativeAddress2(entity.getAdministrativeAddress2());
        dto.setDistance(distance);
        dto.setCareImages(entity.getCareImages().stream().map(CareImgEntity::getImgUrl).collect(Collectors.toList()));
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
    public CarePostDetailsDTO detail(String accessToken, int carePostId, double homeLatitude, double homeLongitude) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        CarePostEntity carePost = carePostRepository.findById(carePostId);
        if (carePost == null) {
            throw new IllegalArgumentException("돌봄글을 찾을 수 없습니다");
        }
        CarePostDetailsDTO dto = new CarePostDetailsDTO();
        dto.setCarePostId(carePost.getId());
        dto.setCaregiverNickname(carePost.getCaregiver().getNickname());
        dto.setCaregiverRating(carePost.getCaregiver().getCareRating());
        dto.setCaregiverReviewCount(carePost.getCaregiver().getCareReviewCount());
        dto.setTitle(carePost.getTitle());
        dto.setContent(carePost.getContent());
        dto.setAdministrativeAddress1(carePost.getAdministrativeAddress1());
        dto.setAdministrativeAddress2(carePost.getAdministrativeAddress2());
        dto.setStreetNameAddress(carePost.getStreetNameAddress());
        dto.setDetailAddress(carePost.getDetailAddress());
        dto.setDistance(getDistance(carePost.getLatitude(), carePost.getLongitude(), homeLatitude, homeLongitude));
        dto.setUnavailableDate(reservationSchedulerRepository.findByCarePost(carePost).stream()
                .map(ReservationSchedulerEntity::getUnavailableDate)
                .collect(Collectors.toList()));
        dto.setCareImages(carePost.getCareImages().stream().map(CareImgEntity::getImgUrl).collect(Collectors.toList()));
        return dto;
    }
    public CarePostDetailsDTO myPost(String accessToken){
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        CarePostEntity carePost = carePostRepository.findByCaregiver_Id(user.getId());
        if (carePost == null) {
            throw new IllegalArgumentException("돌봄글을 찾을 수 없습니다");
        }
        CarePostDetailsDTO dto = new CarePostDetailsDTO();
        dto.setCarePostId(carePost.getId());
        dto.setCaregiverNickname(carePost.getCaregiver().getNickname());
        dto.setCaregiverRating(carePost.getCaregiver().getCareRating());
        dto.setCaregiverReviewCount(carePost.getCaregiver().getCareReviewCount());
        dto.setTitle(carePost.getTitle());
        dto.setContent(carePost.getContent());
        dto.setAdministrativeAddress1(carePost.getAdministrativeAddress1());
        dto.setAdministrativeAddress2(carePost.getAdministrativeAddress2());
        dto.setStreetNameAddress(carePost.getStreetNameAddress());
        dto.setDetailAddress(carePost.getDetailAddress());
        dto.setUnavailableDate(reservationSchedulerRepository.findByCarePost(carePost).stream()
                .map(ReservationSchedulerEntity::getUnavailableDate)
                .collect(Collectors.toList()));
        dto.setCareImages(carePost.getCareImages().stream().map(CareImgEntity::getImgUrl).collect(Collectors.toList()));
        return dto;
    }
    public List<CareApplyDetailDTO> getApplierList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        List<CareMatchingEntity> careMatchingList = careMatchingRepository.findByCarePost_Caregiver_IdAndStatus(user.getId(), 0);

        return careMatchingList.stream()
                .map(this::applierListToDTO)
                .toList();
    }
    private CareApplyDetailDTO applierListToDTO(CareMatchingEntity entity) {
        CareApplyDetailDTO dto = new CareApplyDetailDTO();
        dto.setCareMatchingId(entity.getId());
        dto.setPetName(entity.getPet().getPetName());
        dto.setPetImage(entity.getPet().getPetImage());
        dto.setPetGender(entity.getPet().getGender());
        dto.setPetBirthYear(entity.getPet().getBirthYear());
        dto.setSpecies(entity.getPet().getSpecies());
        dto.setWeight(entity.getPet().getWeight());
        dto.setNeutering(entity.getPet().isNeutering());
        dto.setEtc(entity.getPet().getEtc());
        dto.setAmount(entity.getAmount());
        dto.setReservationStartDate(entity.getReservationStartDate());
        dto.setReservationEndDate(entity.getReservationEndDate());
        dto.setRequestMessage(entity.getRequestMessage());
        return dto;
    }
    public List<CareApplyDetailDTO> getReservationList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        List<CareMatchingEntity> careMatchingList = careMatchingRepository.findByCarePost_Caregiver_IdAndStatus(user.getId(), 1);

        return careMatchingList.stream()
                .map(this::applierListToDTO)
                .toList();
    }
    public  List<CareApplyDetailDTO> getProgressList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        List<CareMatchingEntity> careMatchingList = careMatchingRepository.findByCarePost_Caregiver_IdAndStatus(user.getId(), 2);

        return careMatchingList.stream()
                .map(this::applierListToDTO)
                .toList();
    }
    public List<CareApplyDetailDTO> myApplyList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        List<CareMatchingEntity> careMatchingList = careMatchingRepository.findByOwner_IdAndStatus(user.getId(), 0);

        return careMatchingList.stream()
                .map(this::applierListToDTO)
                .toList();
    }
    public CarePostDetailsDTO reservationPost(String accessToken, int careMatchingId, double homeLatitude, double homeLongitude) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        CareMatchingEntity careMatching = careMatchingRepository.findById(careMatchingId);
        if (careMatching == null) {
            throw new IllegalArgumentException("예약을 찾을 수 없습니다");
        }
        if (careMatching.getOwner().getId() != user.getId()) {
            throw new IllegalStateException("자신의 예약만 확인할 수 있습니다");
        }
        //매칭id로 post정보 가져오기
        CarePostEntity carePost = careMatching.getCarePost();
        CarePostDetailsDTO dto = new CarePostDetailsDTO();
        dto.setCarePostId(carePost.getId());
        dto.setCaregiverNickname(carePost.getCaregiver().getNickname());
        dto.setCaregiverRating(carePost.getCaregiver().getCareRating());
        dto.setCaregiverReviewCount(carePost.getCaregiver().getCareReviewCount());
        dto.setTitle(carePost.getTitle());
        dto.setContent(carePost.getContent());
        dto.setAdministrativeAddress1(carePost.getAdministrativeAddress1());
        dto.setAdministrativeAddress2(carePost.getAdministrativeAddress2());
        dto.setStreetNameAddress(carePost.getStreetNameAddress());
        dto.setDetailAddress(carePost.getDetailAddress());
        dto.setUnavailableDate(reservationSchedulerRepository.findByCarePost(carePost).stream()
                .map(ReservationSchedulerEntity::getUnavailableDate)
                .filter(Objects::nonNull) // null 값을 필터링
                .collect(Collectors.toList()));
        dto.setCareImages(carePost.getCareImages().stream().map(CareImgEntity::getImgUrl).collect(Collectors.toList()));
        return dto;
    }
    public List<CareApplyDetailDTO> myConfirmedList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        List<CareMatchingEntity> careMatchingList = careMatchingRepository.findByOwner_IdAndStatus(user.getId(), 1);

        return careMatchingList.stream()
                .map(this::applierListToDTO)
                .toList();
    }
    public List<CareApplyDetailDTO> myProgressList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        List<CareMatchingEntity> careMatchingList = careMatchingRepository.findByOwner_IdAndStatus(user.getId(), 2);

        return careMatchingList.stream()
                .map(this::applierListToDTO)
                .toList();
    }
    @Transactional
    public void acceptCare(String accessToken, int careMatchingId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        CareMatchingEntity careMatching = careMatchingRepository.findById(careMatchingId);
        if (careMatching == null) {
            throw new IllegalArgumentException("예약신청내역을 찾을 수 없습니다");
        }
        if (careMatching.getCarePost().getCaregiver().getId() != user.getId()) {
            throw new IllegalStateException("자신의 글에 대한 예약만 수락할 수 있습니다");
        }
        if (careMatching.getStatus() != 0) {
            throw new IllegalStateException("이미 수락한 예약입니다");
        }
        //예약 시간이 현재 시간보다 이후여야함
        if (careMatching.getReservationStartDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("예약 시간이 현재 시간보다 이전입니다");
        }
        if(careMatching.getAmount()> careMatching.getOwner().getCoin()){
            throw new IllegalStateException("상대방이 코인이 부족합니다 다른 고객을 선택해주세요");
        }
        // 예약 시작일과 종료일을 LocalDate로 변환하여 날짜 리스트를 만듭니다.
        LocalDate startDate = careMatching.getReservationStartDate().toLocalDate();
        LocalDate endDate = careMatching.getReservationEndDate().toLocalDate();
        List<LocalDate> reservationDates = startDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toList());
        log.info("startDate: {}", startDate);
        log.info("endDate: {}", endDate);
        log.info("reservationDates: {}", reservationDates);

        // 예약 불가 날짜와 겹치는지 확인
        CarePostEntity carePost = careMatching.getCarePost();
        List<ReservationSchedulerEntity> reservationSchedulers = reservationSchedulerRepository.findByCarePost(carePost);
        for (LocalDate date : reservationDates) {
            boolean isUnavailable = reservationSchedulers.stream()
                    .map(ReservationSchedulerEntity::getUnavailableDate)
                    .filter(Objects::nonNull)
                    .anyMatch(unavailableDate -> unavailableDate.toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate().isEqual(date));
            if (isUnavailable) {
                throw new IllegalStateException("예약 기간에 예약 불가 날짜가 포함되어 있습니다.");
            }
        }
        // 예약 스케줄러 업데이트
        for (LocalDate localDate : reservationDates) {
            Date dateToAdd = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            boolean isExist = reservationSchedulerRepository.existsByCarePostAndReservationDate(carePost, dateToAdd);
            if (isExist) {
                ReservationSchedulerEntity reservationSchedulerEntity = reservationSchedulerRepository.findByCarePostAndReservationDate(carePost, dateToAdd);
                int currentReservations = reservationSchedulerEntity.getReservations();
                reservationSchedulerEntity.setReservations(currentReservations + 1);
                if (currentReservations + 1 == 2) {
                    reservationSchedulerEntity.setUnavailableDate(dateToAdd);
                }
                reservationSchedulerRepository.save(reservationSchedulerEntity);
            } else {
                ReservationSchedulerEntity reservation = new ReservationSchedulerEntity();
                reservation.setCarePost(carePost);
                reservation.setReservationDate(dateToAdd);
                reservation.setReservations(1);
                reservationSchedulerRepository.save(reservation);
            }
        }

        careMatching.setStatus(1);
        careMatchingRepository.save(careMatching);
        carePost.setReservations(carePost.getReservations() + 1);
        carePostRepository.save(carePost);
        //예약 수락한거 메세지 보내야함 양쪽
        sseService.notifyMatch(String.valueOf(careMatching.getOwner().getId()), "예약이 수락되었습니다", 1);
        sseService.notifyMatch(String.valueOf(careMatching.getCarePost().getCaregiver().getId()), "예약이 수락되었습니다", 1);

        //결제 - 차감만 하면됌
        careMatching.getOwner().setCoin(careMatching.getOwner().getCoin()-careMatching.getAmount());
    }
    @Transactional
    public void rejectCare(String accessToken, int careMatchingId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        CareMatchingEntity careMatching = careMatchingRepository.findById(careMatchingId);
        if (careMatching == null) {
            throw new IllegalArgumentException("예약신청내역을 찾을 수 없습니다");
        }
        if (careMatching.getCarePost().getCaregiver().getId() != user.getId()) {
            throw new IllegalStateException("자신의 글에 대한 예약만 거절할 수 있습니다");
        }
        if (careMatching.getStatus() != 0) {
            throw new IllegalStateException("이미 수락한 예약입니다");
        }
        careMatchingRepository.delete(careMatching);
        //예약 거절한거 메세지 보내야함
        sseService.notifyMatch(String.valueOf(careMatching.getOwner().getId()), "예약이 거절되었습니다", 5);
    }
    @Transactional
    public void cancelApply(String accessToken, int careMatchingId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        CareMatchingEntity careMatching = careMatchingRepository.findById(careMatchingId);
        if (careMatching == null) {
            throw new IllegalArgumentException("예약신청내역을 찾을 수 없습니다");
        }
        if (careMatching.getOwner().getId() != user.getId()) {
            throw new IllegalStateException("자신의 예약신청만 취소할 수 있습니다");
        }
        if (careMatching.getStatus() != 0) {
            throw new IllegalStateException("이미 수락된 예약은 예약 취소로 진행해주세요");
        }
        careMatchingRepository.delete(careMatching);
    }
    @Transactional
    public void cancelReservation(String accessToken, int careMatchingId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        CareMatchingEntity careMatching = careMatchingRepository.findById(careMatchingId);
        if (careMatching == null) {
            throw new IllegalArgumentException("예약내역을 찾을 수 없습니다");
        }
        if (careMatching.getOwner().getId() != user.getId()) {
            throw new IllegalStateException("자신의 예약만 취소할 수 있습니다");
        }
        if (careMatching.getStatus() != 1) {
            throw new IllegalStateException("확정된 예약건에 대해서만 취소가 가능합니다");
        }
        //당일 예약 취소는 불가능합니다. 24시간 전까지만 취소 가능합니다.
        if(careMatching.getReservationStartDate().isBefore(LocalDateTime.now().plusDays(1))) {
            throw new IllegalStateException("24시간 전까지만 취소 가능합니다.");
        }
        careMatchingRepository.delete(careMatching);
        CarePostEntity carePost = careMatching.getCarePost();
        carePost.setReservations(carePost.getReservations() - 1);
        carePostRepository.save(carePost);

        LocalDate startDate = careMatching.getReservationStartDate().toLocalDate();
        LocalDate endDate = careMatching.getReservationEndDate().toLocalDate();
        List<LocalDate> reservationDates = startDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toList());
        // 예약 스케줄러 업데이트
        for (LocalDate localDate : reservationDates) {
            Date dateToAdd = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            ReservationSchedulerEntity reservationSchedulerEntity = reservationSchedulerRepository.findByCarePostAndReservationDate(carePost, dateToAdd);
            int currentReservations = reservationSchedulerEntity.getReservations();
            if(currentReservations == 2) {
                reservationSchedulerEntity.setUnavailableDate(null);
                reservationSchedulerEntity.setReservations(currentReservations - 1);
                reservationSchedulerRepository.save(reservationSchedulerEntity);
            }
            else if(currentReservations == 1) {
                reservationSchedulerRepository.delete(reservationSchedulerEntity);
            }
        }
        //예약 취소한거 메세지 보내야함
        sseService.notifyMatch(String.valueOf(careMatching.getCarePost().getCaregiver().getId()), "예약이 취소되었습니다", 3);
        //환불
        careMatching.getOwner().setCoin(careMatching.getOwner().getCoin()+careMatching.getAmount());
    }
    @Transactional
    public void startCare(String accessToken, int careMatchingId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        CareMatchingEntity careMatching = careMatchingRepository.findById(careMatchingId);
        if (careMatching == null) {
            throw new IllegalArgumentException("예약내역을 찾을 수 없습니다");
        }
        if (careMatching.getCarePost().getCaregiver().getId() != user.getId()) {
            throw new IllegalStateException("자신의 예약만 시작할 수 있습니다");
        }
        if (careMatching.getStatus() != 1) {
            throw new IllegalStateException("수락되지 않은 예약은 시작할 수 없습니다");
        }
        //예약날짜의 당일 언제든 시작할 수 있습니다.
        if(careMatching.getReservationStartDate().toLocalDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException("예약날짜의 당일에만 시작할 수 있습니다");
        }
        careMatching.setStatus(2);
        careMatching.setStartDate(LocalDateTime.now());
        careMatchingRepository.save(careMatching);
        //돌봄 시작 메세지 보내야함 양쪽
        sseService.notifyMatch(String.valueOf(careMatching.getOwner().getId()), "돌봄이 시작되었습니다", 6);
        sseService.notifyMatch(String.valueOf(careMatching.getCarePost().getCaregiver().getId()), "돌봄이 시작되었습니다", 6);
    }
    @Transactional
    public int completeCare(String accessToken, int careMatchingId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        CareMatchingEntity careMatching = careMatchingRepository.findById(careMatchingId);
        if (careMatching == null) {
            throw new IllegalArgumentException("예약내역을 찾을 수 없습니다");
        }
        if (careMatching.getCarePost().getCaregiver().getId() != user.getId()) {
            throw new IllegalStateException("자신의 예약만 완료할 수 있습니다");
        }
        if (careMatching.getStatus() != 2) {
            throw new IllegalStateException("시작되지 않은 예약은 완료할 수 없습니다");
        }
        CarePostEntity carePost = careMatching.getCarePost();
        carePost.setReservations(carePost.getReservations() - 1);
        carePostRepository.save(carePost);

        CareRecordEntity careRecode = new CareRecordEntity();
        careRecode.setCaregiver(user);
        careRecode.setPet(careMatching.getPet());
        careRecode.setOwner(careMatching.getOwner());
        careRecode.setStatus(3);
        careRecode.setReservationStartDate(careMatching.getReservationStartDate());
        careRecode.setReservationEndDate(careMatching.getReservationEndDate());
        careRecode.setStartDate(careMatching.getStartDate());
        careRecode.setEndDate(LocalDateTime.now());
        careRecode.setAmount(careMatching.getAmount());
        careRecode.setRequestMessage(careMatching.getRequestMessage());
        careRecode.setReview(false);
        careRecordRepository.save(careRecode);
        //돌봄 완료 메세지 보내야함 양쪽
        sseService.notifyMatch(String.valueOf(careMatching.getOwner().getId()), "돌봄이 완료되었습니다", 4);
        sseService.notifyMatch(String.valueOf(careMatching.getCarePost().getCaregiver().getId()), "돌봄이 완료되었습니다", 4);

        return careRecode.getId();
    }
    @Transactional
    public void incompleteCare(String accessToken, int careMatchingId,String reason) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);

        CareMatchingEntity careMatching = careMatchingRepository.findById(careMatchingId);
        if (careMatching == null) {
            throw new IllegalArgumentException("예약내역을 찾을 수 없습니다");
        }
        if (careMatching.getCarePost().getCaregiver().getId() != user.getId()) {
            throw new IllegalStateException("자신의 예약만 완료할 수 있습니다");
        }
        if (careMatching.getStatus() != 2) {
            throw new IllegalStateException("시작되지 않은 예약은 완료할 수 없습니다");
        }
        CareRecordEntity careRecode = new CareRecordEntity();
        careRecode.setCaregiver(user);
        careRecode.setPet(careMatching.getPet());
        careRecode.setOwner(careMatching.getOwner());
        careRecode.setStatus(4);
        careRecode.setReservationStartDate(careMatching.getReservationStartDate());
        careRecode.setReservationEndDate(careMatching.getReservationEndDate());
        careRecode.setStartDate(careMatching.getStartDate());
        careRecode.setEndDate(LocalDateTime.now());
        careRecode.setAmount(careMatching.getAmount());
        careRecode.setRequestMessage(careMatching.getRequestMessage());
        careRecode.setReview(false);
        careRecode.setReason(reason);
        careRecordRepository.save(careRecode);
        //돌봄 미완료 메세지 보내야함 양쪽
        sseService.notifyMatch(String.valueOf(careMatching.getOwner().getId()), "돌봄이 미완료되었습니다", 7);
        sseService.notifyMatch(String.valueOf(careMatching.getCarePost().getCaregiver().getId()), "돌봄이 미완료되었습니다", 7);
    }
}
