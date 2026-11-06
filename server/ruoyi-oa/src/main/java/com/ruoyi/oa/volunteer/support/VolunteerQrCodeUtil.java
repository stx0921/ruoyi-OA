package com.ruoyi.oa.volunteer.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 志愿活动二维码工具：ZXing 生成 300x300 PNG base64
 *
 * <p>内容 = {@code {oa.volunteer.h5-base-url}/volunteer/apply?activityId={id}}
 * （前端 history 路由，不使用 /#/），h5BaseUrl 自动去掉尾部斜杠。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class VolunteerQrCodeUtil
{
    private static final Logger log = LoggerFactory.getLogger(VolunteerQrCodeUtil.class);

    /** 二维码边长（像素） */
    private static final int QR_SIZE = 300;

    /** H5 部署地址（application.yml: oa.volunteer.h5-base-url） */
    @Value("${oa.volunteer.h5-base-url:http://localhost:80}")
    private String h5BaseUrl;

    /**
     * 生成二维码内容（H5 报名页链接）
     */
    public String buildContent(Long activityId)
    {
        String base = StringUtils.trim(h5BaseUrl);
        if (StringUtils.isEmpty(base))
        {
            base = "http://localhost:80";
        }
        while (base.endsWith("/"))
        {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/volunteer/apply?activityId=" + activityId;
    }

    /**
     * 生成二维码：返回 { content, base64 }
     */
    public Map<String, Object> generate(Long activityId)
    {
        String content = buildContent(activityId);
        String base64 = toBase64Png(content);
        Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("content", content);
        result.put("base64", base64);
        return result;
    }

    /**
     * 生成 PNG base64（带 data URI 前缀）
     */
    public String toBase64Png(String content)
    {
        try
        {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            BufferedImage image = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < QR_SIZE; x++)
            {
                for (int y = 0; y < QR_SIZE; y++)
                {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        }
        catch (Exception e)
        {
            log.error("生成活动二维码失败 content={}", content, e);
            throw new ServiceException("二维码生成失败，请稍后重试", 500);
        }
    }

}
