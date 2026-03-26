package com.dtt.simulations.controller;


import com.dtt.simulations.dto.VerifyFaceDto;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.service.Iface.FaceVerificationIface;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@RestController
public class FaceVerificationController {
    private final FaceVerificationIface faceVerificationIface;

    public FaceVerificationController(FaceVerificationIface faceVerificationIface) {
        this.faceVerificationIface = faceVerificationIface;
    }

    @PostMapping("api/post/verify/face")
    public ApiResponse saveBookingDetails(@RequestBody VerifyFaceDto verifyFaceDto) {

        return faceVerificationIface.verifyFace(verifyFaceDto);
    }

}
