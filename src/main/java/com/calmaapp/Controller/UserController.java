package com.calmaapp.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.calmaapp.authentication.TokenBlacklistService;
import com.calmaapp.entity.Review;
import com.calmaapp.entity.Salon;
import com.calmaapp.entity.User;
import com.calmaapp.payloads.*;
import com.calmaapp.repository.SalonRepository;
import com.calmaapp.repository.UserRepository;
import com.calmaapp.service.OTPVerificationService;
import com.calmaapp.service.SalonService;
import com.calmaapp.service.UserService;
import com.calmaapp.userService.UserLoginService;
import com.calmaapp.userService.UserLogoutService;
import com.calmaapp.userService.UserRegistrationService;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRegistrationService userRegistrationService;

    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SalonService salonService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserLogoutService userLogoutService;

    @Autowired
    private SalonRepository salonRepository;

    @Autowired
    private OTPVerificationService otpVerificationService;

    private Logger log;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserDTO userDTO) {
        return userRegistrationService.registerUser(userDTO);
    }

    private String validatePassword(String password) {
        String passwordPattern = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()-_=+{}\\[\\]:;\"'<>,.?/\\\\])[A-Za-z\\d!@#$%^&*()-_=+{}\\[\\]:;\"'<>,.?/\\\\]{8,20}$";
        if (!password.matches(passwordPattern)) {
            throw new IllegalArgumentException(
                    "Password must be between 8 and 20 characters and contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
        }
        return "Password is valid";
    }



    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody UserDTO userDTO) {
        ResponseEntity<Map<String, String>> responseEntity;
        try {
            responseEntity = userLoginService.loginUser(userDTO);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
        return responseEntity;
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(@RequestBody Map<String, String> requestParams) {
        String phoneNumber = requestParams.get("phoneNumber");
        String jwtToken = requestParams.get("jwtToken");

        // Check if the token is blacklisted
        if (tokenBlacklistService.isTokenBlacklisted(jwtToken)) {
            return ResponseEntity.status(401).body("Invalid token. User already logged out.");
        }

        // Logout user (simulate logout)
        userLogoutService.logoutUser(phoneNumber, jwtToken);

        return ResponseEntity.ok("User logged out successfully");
    }

    @PutMapping("/updateLocation/{phoneNumber}")
    public ResponseEntity<String> updateUserLocation(
            @PathVariable String phoneNumber,
            @RequestBody Map<String, Double> location) {
        try {
            // Fetch the user by userId
            User user = userRepository.findByPhoneNumber(phoneNumber);

            if (user != null) {
                // Extract latitude and longitude from the request body
                Double latitude = location.get("latitude");
                Double longitude = location.get("longitude");

                // Update user location
                userService.updateUserLocation(phoneNumber, latitude, longitude);

                return ResponseEntity.ok("User location updated successfully");
            } else {
                return ResponseEntity.status(404).body("User not found");
            }
        } catch (Exception e) {
            // Handle exception
            return ResponseEntity.status(500).body("Failed to update user location");
        }
    }

    @PutMapping("/updateUser/{userId}")
    public ResponseEntity<String> updateUserDetails(
            @PathVariable Long userId,
            @RequestBody UserDTO updatedUserDTO) {
        try {
            // Fetch the user by userId
            User user = userService.getUserById(userId);

            if (user != null) {
                // Update user details
                boolean isUpdated = userService.updateUserDetails(userId, updatedUserDTO);

                if (isUpdated) {
                    return ResponseEntity.ok("User details updated successfully");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to update user details");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
        } catch (Exception e) {

            log.error("Exception while updating user details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update user details");
        }
    }

    @PostMapping("/salons/{salonId}/reviews")
    public ResponseEntity<String> addReviewToSalon(
            @PathVariable Long salonId,
            @RequestBody ReviewDTO reviewDTO,
            Principal principal) { // Include Principal for user authentication
        try {
            User user = userService.getUserByPhoneNumber(principal.getName());
            Salon salon = salonService.getSalonById(salonId);

            System.out.println("User: " + user); // Add this line
            System.out.println("Salon: " + salon); // Add this line

            if (user != null && salon != null) {
                Review review = new Review();
                review.setRating(reviewDTO.getRating());
                review.setComment(reviewDTO.getComment());
                review.setUser(user);
                review.setSalon(salon);

                salon.getReviews().add(review);
                salonRepository.save(salon);

                return ResponseEntity.ok("Review added successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User or salon not found");
            }
        } catch (Exception e) {
            e.printStackTrace(); // Add this line
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add review");
        }
    }

    @GetMapping("/salons/{salonId}/reviews")
    public ResponseEntity<List<ReviewDTO>> getReviewsForSalon(@PathVariable Long salonId) {
        try {
            Salon salon = salonService.getSalonById(salonId);
            if (salon != null) {
                List<ReviewDTO> reviews = salon.getReviews().stream()
                        .map(this::mapToReviewDTO)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(reviews);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private ReviewDTO mapToReviewDTO(Review review) {
        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setRating(review.getRating());
        reviewDTO.setComment(review.getComment());
        return reviewDTO;
    }

    @PostMapping("/salon/update-distance/{salonId}")
    public ResponseEntity<String> updateSalonDistance(@PathVariable Long salonId,
            @RequestParam Double userLatitude,
            @RequestParam Double userLongitude) {
        try {
            return salonService.updateSalonDistance(salonId, userLatitude, userLongitude);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing the request.");
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOTP(@RequestParam String email, @RequestParam String enteredOTP) {
        boolean isOTPVerified = otpVerificationService.verifyOTP(email, enteredOTP);
        if (isOTPVerified) {
            // Proceed with the action (e.g., user registration)
            return ResponseEntity.ok("OTP verification successful");
        } else {
            return ResponseEntity.badRequest().body("Invalid OTP. Please try again.");
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserDetails(@PathVariable Long userId) {
        try {
            // Retrieve user details by userId (Assuming you have a userService to handle this)
            UserDTO userDTO = userService.getUserDetails(userId);

            // Return user details in ResponseEntity
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            // Handle any exceptions and return an error response
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


}
