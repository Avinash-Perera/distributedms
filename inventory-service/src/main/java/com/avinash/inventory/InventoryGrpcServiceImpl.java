package com.avinash.inventory;

import com.avinash.grpc.InventoryGrpcServiceGrpc;
import com.avinash.grpc.InventoryRequest;
import com.avinash.grpc.InventoryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class InventoryGrpcServiceImpl extends InventoryGrpcServiceGrpc.InventoryGrpcServiceImplBase {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Override
    public void checkInventory(InventoryRequest request, StreamObserver<InventoryResponse> responseObserver) {
        String productId = request.getProductId();
        int quantity = request.getQuantity();

        boolean available = inventoryRepository.findById(productId)
                .map(inventory -> inventory.getStock() >= quantity)
                .orElse(false);

        InventoryResponse response = InventoryResponse.newBuilder()
                .setAvailable(available)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
