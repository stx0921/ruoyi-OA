package com.ruoyi.oa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oa.feedback.support.FeedbackFileValidator;

/**
 * 工单附件校验器单元测试（不依赖数据库/Spring 容器）
 *
 * @author ruoyi-oa
 */
class FeedbackFileValidatorTest
{
    private final FeedbackFileValidator validator = new FeedbackFileValidator();

    /**
     * 极简 MultipartFile 桩，避免为测试引入 spring-test 依赖
     */
    private static class StubMultipartFile implements MultipartFile
    {
        private final String name;
        private final byte[] content;

        StubMultipartFile(String name, byte[] content)
        {
            this.name = name;
            this.content = content;
        }

        @Override
        public String getName()
        {
            return "file";
        }

        @Override
        public String getOriginalFilename()
        {
            return name;
        }

        @Override
        public String getContentType()
        {
            return "application/octet-stream";
        }

        @Override
        public boolean isEmpty()
        {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize()
        {
            return content == null ? 0 : content.length;
        }

        @Override
        public byte[] getBytes()
        {
            return content;
        }

        @Override
        public InputStream getInputStream()
        {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException
        {
            throw new UnsupportedOperationException("not needed in test");
        }
    }

    private static byte[] png()
    {
        return new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };
    }

    private static byte[] jpg()
    {
        return new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0 };
    }

    private static byte[] gif()
    {
        return new byte[] { 'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0 };
    }

    private static byte[] webp()
    {
        return new byte[] { 'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P' };
    }

    private static byte[] docx()
    {
        return new byte[] { 'P', 'K', 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0 };
    }

    private static byte[] doc()
    {
        return new byte[] { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A,
                (byte) 0xE1, 0, 0, 0, 0 };
    }

    private MultipartFile file(String name, byte[] bytes)
    {
        return new StubMultipartFile(name, bytes);
    }

    @Test
    void validMagicShouldPass()
    {
        assertEquals("image", validator.validate(file("a.png", png())));
        assertEquals("image", validator.validate(file("a.jpeg", jpg())));
        assertEquals("image", validator.validate(file("a.gif", gif())));
        assertEquals("image", validator.validate(file("a.webp", webp())));
        assertEquals("word", validator.validate(file("a.docx", docx())));
        assertEquals("word", validator.validate(file("a.doc", doc())));
    }

    @Test
    void fakeExtensionShouldBeRejected()
    {
        assertThrows(ServiceException.class, () -> validator.validate(file("evil.png", "hello".getBytes())));
        assertThrows(ServiceException.class, () -> validator.validate(file("evil.docx", "hello".getBytes())));
    }

    @Test
    void unsupportedExtensionShouldBeRejected()
    {
        assertThrows(ServiceException.class, () -> validator.validate(file("evil.exe", docx())));
        assertThrows(ServiceException.class, () -> validator.validate(file("noext", docx())));
    }

    @Test
    void imageOverSizeShouldBeRejected()
    {
        byte[] bytes = Arrays.copyOf(png(), (int) FeedbackFileValidator.IMAGE_MAX_SIZE + 1);
        assertThrows(ServiceException.class, () -> validator.validate(file("big.png", bytes)));
    }

    @Test
    void wordOverSizeShouldBeRejected()
    {
        byte[] bytes = Arrays.copyOf(docx(), (int) FeedbackFileValidator.WORD_MAX_SIZE + 1);
        assertThrows(ServiceException.class, () -> validator.validate(file("big.docx", bytes)));
    }

    @Test
    void emptyFileShouldBeRejected()
    {
        assertThrows(ServiceException.class, () -> validator.validate(file("empty.png", new byte[0])));
    }

    @Test
    void detectFileTypeShouldFallbackToWord()
    {
        assertEquals("image", validator.detectFileType("a.PNG"));
        assertEquals("word", validator.detectFileType("a.docx"));
        assertEquals("word", validator.detectFileType(null));
    }
}
