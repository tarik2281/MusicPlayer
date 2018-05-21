set(CMAKE_INCLUDE_CURRENT_DIR ON)
include_directories(
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/toolkit
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/asf
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/mpeg
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/ogg
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/ogg/flac
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/flac
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/mpc
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/mp4
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/ogg/vorbis
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/ogg/speex
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/ogg/opus
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/mpeg/id3v2
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/mpeg/id3v2/frames
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/mpeg/id3v1
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/ape
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/wavpack
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/trueaudio
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/riff
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/riff/aiff
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/riff/wav
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/mod
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/s3m
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/it
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/xm
)

if(ZLIB_FOUND)
  include_directories(${ZLIB_INCLUDE_DIR})
elseif(HAVE_ZLIB_SOURCE)
  include_directories(${ZLIB_SOURCE})
endif()

if(HAVE_BOOST_BYTESWAP OR HAVE_BOOST_ATOMIC OR HAVE_BOOST_ZLIB)
  include_directories(${Boost_INCLUDE_DIR})
endif()

set(tag_HDRS
  taglib/tag.h
  taglib/fileref.h
  taglib/audioproperties.h
  taglib/taglib_export.h
  taglib/taglib_config.h
  taglib/toolkit/taglib.h
  taglib/toolkit/tstring.h
  taglib/toolkit/tlist.h
  taglib/toolkit/tlist.tcc
  taglib/toolkit/tstringlist.h
  taglib/toolkit/tbytevector.h
  taglib/toolkit/tbytevectorlist.h
  taglib/toolkit/tbytevectorstream.h
  taglib/toolkit/tiostream.h
  taglib/toolkit/tfile.h
  taglib/toolkit/tfilestream.h
  taglib/toolkit/tmap.h
  taglib/toolkit/tmap.tcc
  taglib/toolkit/tpropertymap.h
  taglib/toolkit/trefcounter.h
  taglib/toolkit/tdebuglistener.h
  taglib/mpeg/mpegfile.h
  taglib/mpeg/mpegproperties.h
  taglib/mpeg/mpegheader.h
  taglib/mpeg/xingheader.h
  taglib/mpeg/id3v1/id3v1tag.h
  taglib/mpeg/id3v1/id3v1genres.h
  taglib/mpeg/id3v2/id3v2extendedheader.h
  taglib/mpeg/id3v2/id3v2frame.h
  taglib/mpeg/id3v2/id3v2header.h
  taglib/mpeg/id3v2/id3v2synchdata.h
  taglib/mpeg/id3v2/id3v2footer.h
  taglib/mpeg/id3v2/id3v2framefactory.h
  taglib/mpeg/id3v2/id3v2tag.h
  taglib/mpeg/id3v2/frames/attachedpictureframe.h
  taglib/mpeg/id3v2/frames/commentsframe.h
  taglib/mpeg/id3v2/frames/eventtimingcodesframe.h
  taglib/mpeg/id3v2/frames/generalencapsulatedobjectframe.h
  taglib/mpeg/id3v2/frames/ownershipframe.h
  taglib/mpeg/id3v2/frames/popularimeterframe.h
  taglib/mpeg/id3v2/frames/privateframe.h
  taglib/mpeg/id3v2/frames/relativevolumeframe.h
  taglib/mpeg/id3v2/frames/synchronizedlyricsframe.h
  taglib/mpeg/id3v2/frames/textidentificationframe.h
  taglib/mpeg/id3v2/frames/uniquefileidentifierframe.h
  taglib/mpeg/id3v2/frames/unknownframe.h
  taglib/mpeg/id3v2/frames/unsynchronizedlyricsframe.h
  taglib/mpeg/id3v2/frames/urllinkframe.h
  taglib/mpeg/id3v2/frames/chapterframe.h
  taglib/mpeg/id3v2/frames/tableofcontentsframe.h
  taglib/mpeg/id3v2/frames/podcastframe.h
  taglib/ogg/oggfile.h
  taglib/ogg/oggpage.h
  taglib/ogg/oggpageheader.h
  taglib/ogg/xiphcomment.h
  taglib/ogg/vorbis/vorbisfile.h
  taglib/ogg/vorbis/vorbisproperties.h
  taglib/ogg/flac/oggflacfile.h
  taglib/ogg/speex/speexfile.h
  taglib/ogg/speex/speexproperties.h
  taglib/ogg/opus/opusfile.h
  taglib/ogg/opus/opusproperties.h
  taglib/flac/flacfile.h
  taglib/flac/flacpicture.h
  taglib/flac/flacproperties.h
  taglib/flac/flacmetadatablock.h
  taglib/ape/apefile.h
  taglib/ape/apeproperties.h
  taglib/ape/apetag.h
  taglib/ape/apefooter.h
  taglib/ape/apeitem.h
  taglib/mpc/mpcfile.h
  taglib/mpc/mpcproperties.h
  taglib/wavpack/wavpackfile.h
  taglib/wavpack/wavpackproperties.h
  taglib/trueaudio/trueaudiofile.h
  taglib/trueaudio/trueaudioproperties.h
  taglib/riff/rifffile.h
  taglib/riff/aiff/aifffile.h
  taglib/riff/aiff/aiffproperties.h
  taglib/riff/wav/wavfile.h
  taglib/riff/wav/wavproperties.h
  taglib/riff/wav/infotag.h
  taglib/asf/asffile.h
  taglib/asf/asfproperties.h
  taglib/asf/asftag.h
  taglib/asf/asfattribute.h
  taglib/asf/asfpicture.h
  taglib/mp4/mp4file.h
  taglib/mp4/mp4atom.h
  taglib/mp4/mp4tag.h
  taglib/mp4/mp4item.h
  taglib/mp4/mp4properties.h
  taglib/mp4/mp4coverart.h
  taglib/mod/modfilebase.h
  taglib/mod/modfile.h
  taglib/mod/modtag.h
  taglib/mod/modproperties.h
  taglib/it/itfile.h
  taglib/it/itproperties.h
  taglib/s3m/s3mfile.h
  taglib/s3m/s3mproperties.h
  taglib/xm/xmfile.h
  taglib/xm/xmproperties.h
)

set(mpeg_SRCS
  taglib/mpeg/mpegfile.cpp
  taglib/mpeg/mpegproperties.cpp
  taglib/mpeg/mpegheader.cpp
  taglib/mpeg/xingheader.cpp
)

set(id3v1_SRCS
  taglib/mpeg/id3v1/id3v1tag.cpp
  taglib/mpeg/id3v1/id3v1genres.cpp
)

set(id3v2_SRCS
  taglib/mpeg/id3v2/id3v2framefactory.cpp
  taglib/mpeg/id3v2/id3v2synchdata.cpp
  taglib/mpeg/id3v2/id3v2tag.cpp
  taglib/mpeg/id3v2/id3v2header.cpp
  taglib/mpeg/id3v2/id3v2frame.cpp
  taglib/mpeg/id3v2/id3v2footer.cpp
  taglib/mpeg/id3v2/id3v2extendedheader.cpp
  )

set(frames_SRCS
  taglib/mpeg/id3v2/frames/attachedpictureframe.cpp
  taglib/mpeg/id3v2/frames/commentsframe.cpp
  taglib/mpeg/id3v2/frames/eventtimingcodesframe.cpp
  taglib/mpeg/id3v2/frames/generalencapsulatedobjectframe.cpp
  taglib/mpeg/id3v2/frames/ownershipframe.cpp
  taglib/mpeg/id3v2/frames/popularimeterframe.cpp
  taglib/mpeg/id3v2/frames/privateframe.cpp
  taglib/mpeg/id3v2/frames/relativevolumeframe.cpp
  taglib/mpeg/id3v2/frames/synchronizedlyricsframe.cpp
  taglib/mpeg/id3v2/frames/textidentificationframe.cpp
  taglib/mpeg/id3v2/frames/uniquefileidentifierframe.cpp
  taglib/mpeg/id3v2/frames/unknownframe.cpp
  taglib/mpeg/id3v2/frames/unsynchronizedlyricsframe.cpp
  taglib/mpeg/id3v2/frames/urllinkframe.cpp
  taglib/mpeg/id3v2/frames/chapterframe.cpp
  taglib/mpeg/id3v2/frames/tableofcontentsframe.cpp
  taglib/mpeg/id3v2/frames/podcastframe.cpp
)

set(ogg_SRCS
  taglib/ogg/oggfile.cpp
  taglib/ogg/oggpage.cpp
  taglib/ogg/oggpageheader.cpp
  taglib/ogg/xiphcomment.cpp
)

set(vorbis_SRCS
  taglib/ogg/vorbis/vorbisfile.cpp
  taglib/ogg/vorbis/vorbisproperties.cpp
)

set(flacs_SRCS
  taglib/flac/flacfile.cpp
  taglib/flac/flacpicture.cpp
  taglib/flac/flacproperties.cpp
  taglib/flac/flacmetadatablock.cpp
  taglib/flac/flacunknownmetadatablock.cpp
)

set(oggflacs_SRCS
  taglib/ogg/flac/oggflacfile.cpp
)

set(mpc_SRCS
  taglib/mpc/mpcfile.cpp
  taglib/mpc/mpcproperties.cpp
)

set(mp4_SRCS
  taglib/mp4/mp4file.cpp
  taglib/mp4/mp4atom.cpp
  taglib/mp4/mp4tag.cpp
  taglib/mp4/mp4item.cpp
  taglib/mp4/mp4properties.cpp
  taglib/mp4/mp4coverart.cpp
)

set(ape_SRCS
  taglib/ape/apetag.cpp
  taglib/ape/apefooter.cpp
  taglib/ape/apeitem.cpp
  taglib/ape/apefile.cpp
  taglib/ape/apeproperties.cpp
)

set(wavpack_SRCS
  taglib/wavpack/wavpackfile.cpp
  taglib/wavpack/wavpackproperties.cpp
)

set(speex_SRCS
  taglib/ogg/speex/speexfile.cpp
  taglib/ogg/speex/speexproperties.cpp
)

set(opus_SRCS
  taglib/ogg/opus/opusfile.cpp
  taglib/ogg/opus/opusproperties.cpp
)

set(trueaudio_SRCS
  taglib/trueaudio/trueaudiofile.cpp
  taglib/trueaudio/trueaudioproperties.cpp
)

set(asf_SRCS
  taglib/asf/asftag.cpp
  taglib/asf/asffile.cpp
  taglib/asf/asfproperties.cpp
  taglib/asf/asfattribute.cpp
  taglib/asf/asfpicture.cpp
)

set(riff_SRCS
  taglib/riff/rifffile.cpp
)

set(aiff_SRCS
  taglib/riff/aiff/aifffile.cpp
  taglib/riff/aiff/aiffproperties.cpp
)

set(wav_SRCS
  taglib/riff/wav/wavfile.cpp
  taglib/riff/wav/wavproperties.cpp
  taglib/riff/wav/infotag.cpp
)

set(mod_SRCS
  taglib/mod/modfilebase.cpp
  taglib/mod/modfile.cpp
  taglib/mod/modtag.cpp
  taglib/mod/modproperties.cpp
)

set(s3m_SRCS
  taglib/s3m/s3mfile.cpp
  taglib/s3m/s3mproperties.cpp
)

set(it_SRCS
  taglib/it/itfile.cpp
  taglib/it/itproperties.cpp
)

set(xm_SRCS
  taglib/xm/xmfile.cpp
  taglib/xm/xmproperties.cpp
)

set(toolkit_SRCS
  taglib/toolkit/tstring.cpp
  taglib/toolkit/tstringlist.cpp
  taglib/toolkit/tbytevector.cpp
  taglib/toolkit/tbytevectorlist.cpp
  taglib/toolkit/tbytevectorstream.cpp
  taglib/toolkit/tiostream.cpp
  taglib/toolkit/tfile.cpp
  taglib/toolkit/tfilestream.cpp
  taglib/toolkit/tdebug.cpp
  taglib/toolkit/tpropertymap.cpp
  taglib/toolkit/trefcounter.cpp
  taglib/toolkit/tdebuglistener.cpp
  taglib/toolkit/tzlib.cpp
)

if(NOT WIN32)
  set(unicode_SRCS
    taglib/toolkit/unicode.cpp
  )
endif()

if(HAVE_ZLIB_SOURCE)
  set(zlib_SRCS
    ${ZLIB_SOURCE}/adler32.c
    ${ZLIB_SOURCE}/crc32.c
    ${ZLIB_SOURCE}/inffast.c
    ${ZLIB_SOURCE}/inflate.c
    ${ZLIB_SOURCE}/inftrees.c
    ${ZLIB_SOURCE}/zutil.c
  )
endif()

set(tag_LIB_SRCS
  ${mpeg_SRCS} ${id3v1_SRCS} ${id3v2_SRCS} ${frames_SRCS} ${ogg_SRCS}
  ${vorbis_SRCS} ${oggflacs_SRCS} ${mpc_SRCS} ${ape_SRCS} ${toolkit_SRCS} ${flacs_SRCS}
  ${wavpack_SRCS} ${speex_SRCS} ${trueaudio_SRCS} ${riff_SRCS} ${aiff_SRCS} ${wav_SRCS}
  ${asf_SRCS} ${mp4_SRCS} ${mod_SRCS} ${s3m_SRCS} ${it_SRCS} ${xm_SRCS} ${opus_SRCS}
  ${unicode_SRCS} ${zlib_SRCS}
  taglib/tag.cpp
  taglib/tagunion.cpp
  taglib/fileref.cpp
  taglib/audioproperties.cpp
  taglib/tagutils.cpp
)

add_library(tag ${tag_LIB_SRCS} ${tag_HDRS})

if(ZLIB_FOUND)
  target_link_libraries(tag ${ZLIB_LIBRARIES})
endif()

if(HAVE_BOOST_ATOMIC)
  target_link_libraries(tag ${Boost_ATOMIC_LIBRARY})
endif()

if(HAVE_BOOST_ZLIB)
  target_link_libraries(tag ${Boost_IOSTREAMS_LIBRARY} ${Boost_ZLIB_LIBRARY})
endif()

set_target_properties(tag PROPERTIES
  VERSION ${TAGLIB_SOVERSION_MAJOR}.${TAGLIB_SOVERSION_MINOR}.${TAGLIB_SOVERSION_PATCH}
  SOVERSION ${TAGLIB_SOVERSION_MAJOR}
  INSTALL_NAME_DIR ${LIB_INSTALL_DIR}
  DEFINE_SYMBOL MAKE_TAGLIB_LIB
  LINK_INTERFACE_LIBRARIES ""
  PUBLIC_HEADER "${tag_HDRS}"
)
#if(BUILD_FRAMEWORK)
#  set_target_properties(tag PROPERTIES FRAMEWORK TRUE)
#endif()
#
#install(TARGETS tag
#  FRAMEWORK DESTINATION ${FRAMEWORK_INSTALL_DIR}
#  LIBRARY DESTINATION ${LIB_INSTALL_DIR}
#  RUNTIME DESTINATION ${BIN_INSTALL_DIR}
#  ARCHIVE DESTINATION ${LIB_INSTALL_DIR}
#  PUBLIC_HEADER DESTINATION ${INCLUDE_INSTALL_DIR}/taglib
#)
