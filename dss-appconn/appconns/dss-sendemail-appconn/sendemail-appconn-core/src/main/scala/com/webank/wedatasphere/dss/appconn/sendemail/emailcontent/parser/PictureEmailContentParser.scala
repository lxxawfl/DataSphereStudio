/*
 * Copyright 2019 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.webank.wedatasphere.dss.appconn.sendemail.emailcontent.parser

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.{Base64, UUID}

import com.webank.wedatasphere.dss.appconn.sendemail.email.domain.{AbstractEmail, MultiContentEmail, PngAttachment}
import com.webank.wedatasphere.dss.appconn.sendemail.emailcontent.domain.PictureEmailContent
import org.apache.linkis.common.conf.Configuration
import javax.imageio.ImageIO
import org.apache.commons.codec.binary.Base64OutputStream
import com.webank.wedatasphere.dss.appconn.sendemail.conf.SendEmailAppConnConfiguration._

object PictureEmailContentParser extends AbstractEmailContentParser[PictureEmailContent] {

  override protected def parseEmailContent(emailContent: PictureEmailContent,
                                           multiContentEmail: MultiContentEmail): Unit = {
    getFirstLineRecord(emailContent).foreach { imageStr =>
      val decoder = Base64.getDecoder
      val byteArr = decoder.decode(imageStr)
      val inputStream = new ByteArrayInputStream(byteArr)
      val image = ImageIO.read(inputStream)
      val contents = generateImage(image, multiContentEmail)
      emailContent.setContent(contents)
    }
  }

  protected def generateImage(bufferedImage: BufferedImage, email: AbstractEmail): Array[String] = {
    val imageUUID: String = UUID.randomUUID.toString
    val width: Int = bufferedImage.getWidth
    val height: Int = bufferedImage.getHeight
    // 只支持修改visualis图片大小，后续如果有新增其他类型的邮件需要修改图片大小，需要在if中加上该邮件类型
    val imagesCuts = if (email.getEmailType.contains("visualis") && height > EMAIL_IMAGE_HEIGHT.getValue) {
      val numOfCut = Math.ceil(height.toDouble / EMAIL_IMAGE_HEIGHT.getValue).toInt
      val realHeight = height / numOfCut
      (0 until numOfCut).map(i => bufferedImage.getSubimage(0, i * realHeight, width, realHeight)).toArray
    } else Array(bufferedImage)
    imagesCuts.indices.map { index =>
      val image = imagesCuts(index)
      val imageName = index + "_" + imageUUID + ".png"
      val os = new ByteArrayOutputStream
      val b64Stream = new Base64OutputStream(os)
      ImageIO.write(image, "png", b64Stream)
      val b64 = os.toString(Configuration.BDP_ENCODING.getValue)
      email.addAttachment(new PngAttachment(imageName, b64))

      var iHeight = image.getHeight
      var iWidth = image.getWidth

      if (email.getEmailType.contains("visualis") && iWidth > EMAIL_IMAGE_WIDTH.getValue) {
        iHeight = ((EMAIL_IMAGE_WIDTH.getValue.toDouble / iWidth.toDouble) * iHeight.toDouble).toInt
        iWidth = EMAIL_IMAGE_WIDTH.getValue
      }
      s"""<img width="${iWidth}" height="${iHeight}" src="cid:$imageName"></img>"""
    }.toArray
  }

}
