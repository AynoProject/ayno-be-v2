package com.ayno.aynobe.config.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.*;

/**
 * 업로드된 원본 이미지를 주어진 가로 폭들(예: 320/800/1600)에 맞춰
 * 비율을 유지하며 JPEG 바이트 배열로 리사이즈한다.
 *
 * - 입력: 원본 바이트, 생성할 가로폭 목록
 * - 출력: {가로폭 → JPEG 바이트} 맵
 * - 품질을 더 제어하려면 ImageWriter/JPEGImageWriteParam으로 확장 가능
 */
@Component
public final class ImageResizer {

    public Map<Integer, byte[]> resizeSet(byte[] originalImageBytes, List<Integer> targetWidths) {
        try (var in = new ByteArrayInputStream(originalImageBytes)) {
            BufferedImage sourceImage = ImageIO.read(in);
            if (sourceImage == null) {
                throw new IOException("이미지 디코딩 실패 (지원하지 않는 포맷이거나 손상된 파일)");
            }

            Map<Integer, byte[]> result = new LinkedHashMap<>();

            for (int requestedWidth : targetWidths) {
                // 원본보다 큰 폭은 의미 없으므로 원본 폭을 상한으로 사용
                int outputWidth = Math.min(requestedWidth, sourceImage.getWidth());

                // 비율 유지: height = width * (원본세로/원본가로)
                int outputHeight = (int) Math.round(
                        sourceImage.getHeight() * (outputWidth / (double) sourceImage.getWidth())
                );

                // RGB 24bit로 새 캔버스 생성 (알파 불필요 → TYPE_INT_RGB)
                BufferedImage resizedImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);

                // 고급 보간으로 스케일링
                Graphics2D g = resizedImage.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(sourceImage, 0, 0, outputWidth, outputHeight, null);
                g.dispose();

                // JPEG 인코딩
                try (var baos = new ByteArrayOutputStream()) {
                    ImageIO.write(resizedImage, "jpg", baos);
                    result.put(requestedWidth, baos.toByteArray());
                }
            }

            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 리사이즈 중 오류", e);
        }
    }
}