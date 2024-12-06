package tukano.impl.storage;

import static tukano.api.Result.ErrorCode.INTERNAL_ERROR;
import static tukano.api.Result.ErrorCode.TIMEOUT;
import static tukano.api.Result.error;
import static tukano.api.Result.ok;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.util.function.Consumer;
import java.util.function.Supplier;
import tukano.api.Result;
import tukano.api.Result.ErrorCode;
import utils.Sleep;

public class RemoteStorage implements BlobStorage {

    private static final String STORAGE_HOST = "storage";
    private static final String STORAGE_PORT = "8081";
    private static final String STORAGE_URL = String.format("http://%s:%s/rest/blobs", STORAGE_HOST, STORAGE_PORT);
    private static final String STORAGE_TOKEN = ""; // TODO

    protected static final int MAX_RETRIES = 4;
    protected static final int RETRY_SLEEP = 1000;

    private Client client;

    protected RemoteStorage() {
        client = jakarta.ws.rs.client.ClientBuilder.newClient();
    }

    private Result<Void> _write(String path, byte[] bytes) {
        Response res = client
                .target(STORAGE_URL)
                .path(path.replace("/", "")) // TODO replacement
                .request()
                .header("Authorization", STORAGE_TOKEN)
                .post(Entity.entity(bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        return toJavaResult(res);
    }

    private Result<byte[]> _read(String path) {
        Response res = client
                .target(STORAGE_URL)
                .path(path.replace("/", "")) // TODO replacement
                .request()
                .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("Authorization", STORAGE_TOKEN)
                .get();

        return toJavaResult(res, byte[].class);
    }

    private Result<Void> _delete(String path) {
        Response res = client
                .target(STORAGE_URL)
                .path(path.replace("/", "")) // TODO replacement
                .request()
                .header("Authorization", STORAGE_TOKEN)
                .delete();

        return toJavaResult(res);
    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        return reTry(() -> _write(path, bytes));
    }

    @Override
    public Result<byte[]> read(String path) {
        return reTry(() -> _read(path));
    }

    @Override
    public Result<Void> delete(String path) {
        return reTry(() -> _delete(path));
    }

    @Override
    public Result<Void> read(String path, Consumer<byte[]> sink) {
        throw new UnsupportedOperationException("Unimplemented method 'read'");
    }

    private <T> Result<T> reTry(Supplier<Result<T>> func) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return func.get();
            } catch (ProcessingException ex) {
                ex.printStackTrace();
                Sleep.ms(RETRY_SLEEP);
            } catch (Exception ex) {
                ex.printStackTrace();
                return Result.error(INTERNAL_ERROR);
            }
        }
        return Result.error(TIMEOUT);
    }

    private Result<Void> toJavaResult(Response res) {
        try {
            Status st = res.getStatusInfo().toEnum();
            if (st == Status.OK && res.hasEntity()) {
                return ok(null);
            } else if (st == Status.NO_CONTENT) {
                return ok();
            }

            return error(getErrorCodeFrom(st.getStatusCode()));
        } finally {
            res.close();
        }
    }

    private <T> Result<T> toJavaResult(Response res, Class<T> entityType) {
        try {
            Status st = res.getStatusInfo().toEnum();
            if (st == Status.OK && res.hasEntity()) {
                return ok(res.readEntity(entityType));
            } else if (st == Status.NO_CONTENT) {
                return ok();
            }

            return error(getErrorCodeFrom(st.getStatusCode()));
        } finally {
            res.close();
        }
    }

    private ErrorCode getErrorCodeFrom(int st) {
        return switch (st) {
            case 200, 204 -> ErrorCode.OK;
            case 409 -> ErrorCode.CONFLICT;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 400 -> ErrorCode.BAD_REQUEST;
            case 500 -> ErrorCode.INTERNAL_ERROR;
            case 501 -> ErrorCode.NOT_IMPLEMENTED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
