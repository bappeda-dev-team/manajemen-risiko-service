package cc.kertaskerja.manrisk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(
                200,
                message,
                data
        );
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(
                201,
                "Created successfully",
                data
        );
    }

    public static <T> ApiResponse<T> updated(T data) {
        return new ApiResponse<>(
                200,
                "Updated successfully",
                data
        );
    }

    public static <T> ApiResponse<T> deleted() {
        return new ApiResponse<>(
                200,
                "Deleted successfully",
                null
        );
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(
                status,
                message,
                null
        );
    }

    public static <T> ApiResponse<T> error(int status, T data, String message) {
        return new ApiResponse<>(
                status,
                message,
                data
        );
    }
}